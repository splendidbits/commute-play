package pushservices.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import services.splendidlog.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import pushservices.enums.FailureType;
import pushservices.helpers.PlatformHelper;
import pushservices.helpers.TaskHelper;
import pushservices.interfaces.PlatformMessageResponse;
import pushservices.models.app.GcmResponse;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.RecipientFailure;
import pushservices.serializers.GcmMessageSerializer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Sends GCM alerts to a batch of given tokens and data.
 * <p>
 * - This class just deals with GCM concepts:
 * - A list of registration_ids
 * - A collapse_key
 * - The keys and string data to send.
 */
class GcmMessageDispatcher extends PlatformMessageDispatcher {
    private static final int ENDPOINT_REQUEST_TIMEOUT_SECONDS = 30 * 1000;
    private static final int MESSAGE_RECIPIENT_BATCH_SIZE = 900;

    @Inject
    private WSClient mWsClient;

    @Inject
    protected GcmMessageDispatcher() {

    }

    /**
     * Dispatch a message synchronously to the Google GCM service.
     *
     * @param message          The message to send.
     * @param responseListener The response listener.
     */
    @SuppressWarnings("Convert2Lambda")
    public void dispatchMessage(@Nonnull Message message, @Nonnull PlatformMessageResponse responseListener) {

        // Return error on no recipients.
        if (message.recipients == null || message.recipients.isEmpty()) {
            responseListener.messageFailure(message, FailureType.MESSAGE_REGISTRATIONS_MISSING);
            return;
        }

        // Return error on no platform.
        if (message.credentials == null || message.credentials.platformType == null) {
            responseListener.messageFailure(message, FailureType.PLATFORM_AUTH_INVALID);
            return;
        }

        // Build the exception handler block.
        Function<Throwable, Void> exceptionHandler = new Function<Throwable, Void>() {

            @Override
            public Void apply(Throwable exception) {
                if (exception != null && exception instanceof PlatformEndpointException) {
                    Logger.error("Exception dispatching GCM message", exception);

                    PlatformEndpointException googleException = (PlatformEndpointException) exception;
                    if (googleException.mStatusCode == 400) {
                        responseListener.messageFailure(message, FailureType.MESSAGE_PAYLOAD_INVALID);

                    } else if (googleException.mStatusCode == 401) {
                        responseListener.messageFailure(message, FailureType.PLATFORM_AUTH_INVALID);

                    } else if (googleException.mStatusCode == 500) {
                        RecipientFailure recipientFailure = new RecipientFailure();
                        recipientFailure.failTime = Calendar.getInstance();
                        recipientFailure.type = FailureType.PLATFORM_UNAVAILABLE;

                        MessageResult result = new MessageResult();
                        result.addFailedRecipients(message.recipients, recipientFailure);
                        responseListener.messageResult(message, result);

                    } else {
                        responseListener.messageFailure(message, FailureType.ERROR_UNKNOWN);
                    }
                }
                return null;
            }
        };

        // Split the recipients into "parts" as it could be over the max size for a platform message batch.
        List<List<Recipient>> recipientBlocks = splitMessageRecipients(message);

        // Get the accounts for which we are sending the message.
        CompletionStage<List<GcmResponse>> googleResponses = getMessageResponses(message, recipientBlocks);
        googleResponses.thenApply(new Function<List<GcmResponse>, Void>() {

            @Override
            public Void apply(List<GcmResponse> gcmResponses) {
                // The response from each message send.
                MessageResult messageCompleteResult = combineMessageResponses(message, gcmResponses);
                responseListener.messageResult(message, messageCompleteResult);

                return null;
            }
        }).exceptionally(exceptionHandler);
    }

    /**
     * Get a list of Google message responses for a given list of message parts.
     *
     * @param message The message to send and  get responses for.
     * @return A list of Google GCM responses.
     */
    private CompletionStage<List<GcmResponse>> getMessageResponses(@Nonnull Message message,
                                                                   @Nonnull List<List<Recipient>> recipientBlocks) {

        CompletableFuture<List<GcmResponse>> responseFuture = new CompletableFuture<>();
        List<GcmResponse> messagePartResponses = new ArrayList<>();

        for (List<Recipient> block : recipientBlocks) {
            sendMessageRequest(message, block).thenApply(new Function<WSResponse, Void>() {

                @Override
                public Void apply(WSResponse response) {
                    GcmResponse gcmResponse = parseCallResponse(response);

                    messagePartResponses.add(gcmResponse);
                    if (messagePartResponses.size() == recipientBlocks.size()) {
                        responseFuture.complete(messagePartResponses);
                    }
                    return null;
                }
            });
        }
        return responseFuture;
    }

    /**
     * Internally send a message using the GCM protocol to google. If a message contains
     * more than 1000 registration ids, it'll split that into multiples messages.
     *
     * @param message the message.
     * @return A list of organised recipients in blocks of 1000.
     */
    private List<List<Recipient>> splitMessageRecipients(@Nonnull Message message) {
        List<Recipient> messageRecipients = message.recipients;
        List<List<Recipient>> recipientBlocks = new ArrayList<>();

        if (messageRecipients != null && !messageRecipients.isEmpty()) {

            // Add each registration_id to the message in batches of 1000.
            List<Recipient> recipientBlock = new ArrayList<>();
            for (int i = 0; i < messageRecipients.size(); i++) {
                Recipient recipient = messageRecipients.get(i);

                // If there's ~1000 registrations, add the message to the pending messages
                if (recipientBlock.size() == MESSAGE_RECIPIENT_BATCH_SIZE) {
                    recipientBlocks.add(recipientBlock);
                    recipientBlock = new ArrayList<>();
                }

                // Add the registration to the new message.
                if (TaskHelper.isRecipientReady(recipient)) {
                    recipientBlock.add(recipient);
                }

                // Final registration in iteration? Begin processing the batches.
                if (i == (messageRecipients.size() - 1) && !recipientBlock.isEmpty()) {
                    recipientBlocks.add(recipientBlock);
                }
            }
        }
        return recipientBlocks;
    }

    /**
     * Send a message and get a synchronous application response in return.
     *
     * @param message    the message or message part to send.
     * @param recipients blocks of 1000 recipients inside a collection.
     * @return WSResponse google request response.
     */
    @Nonnull
    private CompletionStage<WSResponse> sendMessageRequest(@Nonnull Message message,
                                                           @Nonnull List<Recipient> recipients) {
        String jsonBody = new GsonBuilder()
                .registerTypeAdapter(Message.class, new GcmMessageSerializer(recipients))
                .create()
                .toJson(message);

        return mWsClient
                .url(message.credentials.platformType.url)
                .setContentType("application/json")
                .setHeader("Authorization", String.format("key=%s", message.credentials.authorisationKey))
                .setRequestTimeout(ENDPOINT_REQUEST_TIMEOUT_SECONDS)
                .setFollowRedirects(true)
                .post(jsonBody);
    }

    /**
     * Parse an incoming response back from a GCM send action.
     *
     * @param callResponse PlatformMessageResult The WSResponse back from google which
     *                     should contain a json success / fail map.
     * @return MessageResult result from google for gcm message.
     */
    @Nonnull
    private GcmResponse parseCallResponse(@Nonnull WSResponse callResponse) {
        if (callResponse.getStatus() != 200) {
            throw new PlatformEndpointException(callResponse.getStatus(), callResponse.getBody());
        }

        GcmResponse response = new Gson().fromJson(callResponse.getBody(), GcmResponse.class);
        Logger.debug(String.format("[%1$d] canonical ids.\n[%2$d] successful messages.\n[%3$d] failed messages.",
                response.mCanonicalIdCount, response.mSuccessCount, response.mFailCount));

        return response;
    }

    /**
     * Combine a list of Google response for a given list of responses for messages sent.
     *
     * @param message   The original (non part) message sent to the pushservices.
     * @param responses A list of received GoogleResponses.
     * @return The master GoogleResponse for the original message send back to the client.
     */
    @Nonnull
    private MessageResult combineMessageResponses(@Nonnull Message message, @Nonnull List<GcmResponse> responses) {
        MessageResult messageResult = new MessageResult();

        if (message.recipients != null) {
            int runningRegCounter = 0;

            // Loop through each message response part.
            for (GcmResponse gcmResponse : responses) {

                // Loop through each response from each registration.
                for (int i = 0; i < gcmResponse.mResults.size(); i++) {
                    GcmResponse.ResultData resultData = gcmResponse.mResults.get(i);
                    Recipient originalRecipient = message.recipients.get(runningRegCounter);

                    // A successful message.
                    if (resultData.messageId != null && !resultData.messageId.isEmpty()) {
                        messageResult.addSuccessfulRecipient(originalRecipient);
                    }

                    // Check for changed registration token.
                    if (resultData.registrationId != null && !resultData.registrationId.isEmpty()) {
                        Recipient updatedRecipient = new Recipient(resultData.registrationId);
                        messageResult.addUpdatedRegistration(originalRecipient, updatedRecipient);
                    }

                    // Check for recipient errors.
                    if (resultData.error != null && !resultData.error.isEmpty()) {
                        FailureType failureType = PlatformHelper.getGcmFailureType(resultData.error);
                        RecipientFailure recipientFailure = new RecipientFailure(failureType, resultData.error);

                        // Add the error for that particular registration
                        messageResult.addFailedRecipients(originalRecipient, recipientFailure);
                    }
                }

                // Bump the master registration counter for all parts.
                runningRegCounter++;
            }
        }
        return messageResult;
    }
}
