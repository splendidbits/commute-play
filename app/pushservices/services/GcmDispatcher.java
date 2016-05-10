package pushservices.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import pushservices.enums.PlatformFailureType;
import pushservices.helpers.MessageHelper;
import pushservices.interfaces.RawMessageResponse;
import pushservices.models.app.GoogleResponse;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.types.PushFailCause;
import serializers.GcmMessageSerializer;
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
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
class GcmDispatcher {
    private static final String TAG = GcmDispatcher.class.getSimpleName();
    private static final int GCM_REQUEST_TIMEOUT = 10 * 1000; // 5 seconds

    private WSClient mWsClient;
    private Log mLog;

    @Inject
    public GcmDispatcher(Log log, WSClient wsClient) {
        mWsClient = wsClient;
        mLog = log;
    }

    /**
     * Dispatch a message synchronously to the Google GCM service..
     *
     * @param message          The message to send.
     * @param responseListener The response listener.
     */
    @SuppressWarnings("Convert2Lambda")
    public void dispatchMessageAsync(@Nonnull Message message,
                                     @Nonnull RawMessageResponse responseListener) {

        // Return error on no recipients.
        if (message.recipients == null || message.recipients.isEmpty()) {
            responseListener.messageFailure(message, PushFailCause.MISSING_RECIPIENTS);
            return;
        }

        // Return error on no platform.
        if (message.credentials == null || message.credentials.platformType == null) {
            responseListener.messageFailure(message, PushFailCause.MISSING_CREDENTIALS);
            return;
        }

        // Build the exception handler block.
        Function<Throwable, Void> exceptionHandler = new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable exception) {
                if (exception != null) {
                    mLog.e(TAG, "Error dispatching GCM message", exception);

                    if (exception instanceof GoogleCallException) {
                        GoogleCallException googleException = (GoogleCallException) exception;
                        switch (googleException.mStatusCode) {

                            case 400:
                                responseListener.messageFailure(message,
                                        PushFailCause.GCM_MESSAGE_JSON_ERROR);
                            case 401:
                                responseListener.messageFailure(message,
                                        PushFailCause.AUTH_ERROR);
                            default:
                                responseListener.messageFailure(message,
                                        PushFailCause.UNKNOWN_ERROR);
                                break;
                        }
                    }
                }
                return null;
            }
        };

        // Split the message into "parts" as it could be over the max size for a platform message batch.
        List<Message> messageParts = organiseMessageParts(message);

        // Get the accounts for which we are sending the message.
        CompletionStage<List<GoogleResponse>> googleResponses = getMessageResponses(messageParts);
        googleResponses.thenApply(new Function<List<GoogleResponse>, Void>() {

            @Override
            public Void apply(List<GoogleResponse> googleResponses) {
                // The response from each message send.
                MessageResult messageCompleteResult = combineMessageResponses(message, googleResponses);
                responseListener.messageResult(message, messageCompleteResult);

                return null;
            }
        }).exceptionally(exceptionHandler);
    }

    /**
     * Get a list of Google message responses for a given list of message parts.
     *
     * @param messageParts The list of parts to get responses for.
     * @return A list of google reposes.
     */
    private CompletionStage<List<GoogleResponse>> getMessageResponses(@Nonnull List<Message> messageParts) {

        CompletableFuture<List<GoogleResponse>> responseFuture = new CompletableFuture<>();
        List<GoogleResponse> messagePartResponses = new ArrayList<>();
        for (Message messagePart : messageParts) {

            sendMessageRequest(messagePart).thenApply(new Function<WSResponse, Void>() {
                @Override
                public Void apply(WSResponse response) {
                    GoogleResponse googleResponse = parseCallResponse(response);
                    messagePartResponses.add(googleResponse);

                    if (messagePartResponses.size() == messageParts.size()) {
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
     * @param originalMessage the client message sent to the pushservices.
     * @return A list of organised parts.
     */
    private List<Message> organiseMessageParts(@Nonnull Message originalMessage) {
        List<Message> returnedMessageParts = new ArrayList<>();
        List<Recipient> allRecipients = originalMessage.recipients;

        Message messagePart = MessageHelper.copyMessage(originalMessage);
        int messageRegistrations = 1;

        // Add each registration_id to the message in batches of 1000.
        if (allRecipients != null && !allRecipients.isEmpty()) {
            for (int regCount = 0; regCount < allRecipients.size(); regCount++) {

                // If there's ~1000 registrations, add the message to the pending messages
                if (messageRegistrations == 999) {
                    returnedMessageParts.add(messagePart);

                    messagePart = MessageHelper.copyMessage(originalMessage);
                    messageRegistrations = 0;
                }

                // Add the registration to the new message.
                messagePart.addRecipient(allRecipients.get(regCount));
                messageRegistrations++;

                // Final registration in iteration? Begin processing the batches.
                if (regCount == (allRecipients.size() - 1) && messageRegistrations > 0) {
                    returnedMessageParts.add(messagePart);
                }
            }
        }
        return returnedMessageParts;
    }

    /**
     * Send a message and get a synchronous application response in return.
     *
     * @param messagePart the message or message part to send.
     * @return WSResponse google request response.
     */
    @Nullable
    private CompletionStage<WSResponse> sendMessageRequest(@Nonnull Message messagePart) {
        if (messagePart.credentials != null && messagePart.credentials.platformType != null) {
            String jsonBody = new GsonBuilder()
                    .registerTypeAdapter(Message.class, new GcmMessageSerializer())
                    .create()
                    .toJson(messagePart);

            return mWsClient
                    .url(messagePart.credentials.platformType.url)
                    .setContentType("application/json")
                    .setHeader("Authorization", String.format("key=%s",messagePart.credentials.authorisationKey))
                    .setRequestTimeout(GCM_REQUEST_TIMEOUT)
                    .setFollowRedirects(true)
                    .post(jsonBody);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Parse an incoming response back from a GCM send action.
     *
     * @param callResponse PlatformMessageResult The WSResponse back from google which
     *                     should contain a json success / fail map.
     * @return MessageResult result from google for gcm message.
     */
    @Nonnull
    private GoogleResponse parseCallResponse(@Nonnull WSResponse callResponse) {

        if (callResponse.getStatus() != 200) {
            throw new GoogleCallException(callResponse.getStatusText(), callResponse.getStatus());
        }

        GoogleResponse response = new Gson().fromJson(callResponse.getBody(), GoogleResponse.class);
        mLog.d(TAG, String.format("%d canonical ids.", response.mCanonicalIdCount));
        mLog.d(TAG, String.format("%d successful GCM messages.", response.mSuccessCount));
        mLog.d(TAG, String.format("%d failed GCM messages.", response.mFailCount));

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
    private MessageResult combineMessageResponses(@Nonnull Message message, @Nonnull List<GoogleResponse> responses) {
        MessageResult messageResult = new MessageResult();

        // Start the registrationCount before all of the message parts so inbound == outbound
        int runningRegCounter = 0;

        // Loop through each message response part.
        for (GoogleResponse googleResponse : responses) {

            // Loop through each response from each registration.
            for (int i = 0; i < googleResponse.mResults.size(); i++) {
                GoogleResponse.ResultData resultData = googleResponse.mResults.get(i);
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

                // Check for errors.
                if (resultData.error != null && !resultData.error.isEmpty()) {

                    // Add the error for that particular registration
                    messageResult.addErrorToRecipient(originalRecipient, resultData.error);
                    for (PlatformFailureType error : PlatformFailureType.values()) {

                        // Check for an error in the response where the recipient is stale.
                        if (error.friendlyError != null &&
                                error.friendlyError.equals(PlatformFailureType.ERROR_NOT_REGISTERED) ||
                                error.equals(PlatformFailureType.ERROR_INVALID_REGISTRATION)) {
                            messageResult.addStaleRecipient(originalRecipient);
                        }
                    }
                }

                // Bump the master registration counter for all parts.
                runningRegCounter++;
            }
        }

        return messageResult;
    }

    /**
     * Thrown on a hard, breaking http call error such as 404.
     */
    private class GoogleCallException extends RuntimeException {
        int mStatusCode = -1;
        String mErrorMessage;

        GoogleCallException(String errorMessage, int statusCode) {
            mErrorMessage = errorMessage;
            mStatusCode = statusCode;
        }
    }
}
