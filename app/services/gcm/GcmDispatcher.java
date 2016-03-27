package services.gcm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.GcmMessageSerializer;
import interfaces.IPushResponse;
import main.Log;
import models.accounts.PlatformAccount;
import models.app.GoogleResponse;
import models.app.MessageResult;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import play.api.Play;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Sends GCM alerts to a batch of given tokens and data.
 * <p>
 * - This class just deals with GCM concepts:
 * - A list of registration_ids
 * - A collapse_key
 * - The keys and string data to send.
 */
public class GcmDispatcher {
    private static final String TAG = GcmDispatcher.class.getSimpleName();
    private static final int GCM_REQUEST_TIMEOUT = 5 * 1000; // 5 seconds

    private ConcurrentHashMap<PlatformAccount, List<Message>> mOutboundMessages = new ConcurrentHashMap<>();
    private List<GoogleResponse> mInboundResponses = Collections.synchronizedList(new ArrayList<>());

    private IPushResponse mResponseInterface = null;
    private Message mOriginalMessage = null;

    private WSClient mWsClient = Play.current().injector().instanceOf(WSClient.class);

    /**
     * Send a originalMessage to Google.
     *
     * @param originalMessage The push originalMessage.
     * @param gcmResponse     Response interface.
     */
    public GcmDispatcher(@Nonnull Message originalMessage, @Nonnull IPushResponse gcmResponse) {
        mResponseInterface = gcmResponse;
        mOriginalMessage = originalMessage;

        // Kick off the jobs asynchronously.
        organiseMessage().thenApplyAsync(new Function<Boolean, Void>() {
            @Override
            public Void apply(Boolean aBoolean) {
                sendMessages();
                return null;
            }
        });
    }

    /**
     * Internally send a message using the GCM protocol to google.
     * If a message contains more than 1000 registration ids,
     * it'll split that into multiples messages.
     */
    private CompletionStage<Boolean> organiseMessage() {
        List<Recipient> allRecipients = mOriginalMessage.recipients;

        Message messagePart = cloneMessage(mOriginalMessage);
        int messageRegistrations = 0;

        // Add each registration_id to the message in batches of 1000.
        if (allRecipients != null) {
            for (int regCount = 0; regCount < allRecipients.size(); regCount++) {

                // If there's ~1000 registrations, add the message to the pending messages
                if (messageRegistrations == 1000) {
                    addOutboundMessage(messagePart);

                    messagePart = cloneMessage(mOriginalMessage);
                    messageRegistrations = 0;
                }

                // Add the registration to the new message.
                messagePart.recipients.add(allRecipients.get(regCount));
                messageRegistrations++;

                // Final registration in iteration? Start processing the batches
                if (regCount == (allRecipients.size() - 1) && messageRegistrations > 0) {
                    addOutboundMessage(messagePart);
                }
            }
        }

        return CompletableFuture.completedFuture(messageRegistrations > 0);
    }

    /**
     * Add a message to the outbound GCM queue.
     *
     * @param message add message to outbound gcm account queue.
     */
    private void addOutboundMessage(@Nonnull Message message) {
        if (message.platformAccount != null) {
            if (mOutboundMessages.containsKey(message.platformAccount)) {
                mOutboundMessages.get(message.platformAccount).add(message);

            } else {
                List<Message> accountMessages = new ArrayList<>();
                accountMessages.add(message);
                mOutboundMessages.put(message.platformAccount, accountMessages);
            }
        }
    }

    /**
     * Send all message parts in the queue for all accounts.
     */
    private CompletionStage<Void> sendMessages() {
        // Loop through each platform listed for the account and look for the GCM.
        Iterator<Map.Entry<PlatformAccount, List<Message>>> platformIterator = mOutboundMessages.entrySet().iterator();
        while (platformIterator.hasNext()) {

            Map.Entry<PlatformAccount, List<Message>> platformValues = platformIterator.next();
            PlatformAccount platformAccount = platformValues.getKey();
            List<Message> platformMessages = platformValues.getValue();

            for (Message message : platformMessages) {
                String jsonBody = new GsonBuilder()
                        .registerTypeAdapter(Message.class, new GcmMessageSerializer())
                        .create()
                        .toJson(message);

                WSRequest request = mWsClient
                        .url(platformAccount.platform.endpointUrl)
                        .setFollowRedirects(true)
                        .setContentType("application/json")
                        .setRequestTimeout(GCM_REQUEST_TIMEOUT)
                        .setHeader("Authorization", String.format("key=%s", platformAccount.authToken));

                CompletionStage<WSResponse> result = request
                        .post(jsonBody);

                return result.thenApply(new Function<WSResponse, Void>() {
                    @Override
                    public Void apply(WSResponse wsResponse) {
                        parseResponse(wsResponse);
                        return null;
                    }
                });
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Parse an incoming response back from a GCM send action.
     *
     * @param requestResponse PlatformMessageResult The WSResponse back from google which
     *                        should contain a json success / fail map.
     * @return MessageResult result from google for gcm message.
     */
    private void parseResponse(@Nonnull WSResponse requestResponse) {
        GoogleResponse response = new Gson().fromJson(requestResponse.getBody(), GoogleResponse.class);
        mInboundResponses.add(response);

        Log.d(TAG, String.format("%d canonical ids.", response.mCanonicalIdCount));
        Log.d(TAG, String.format("%d successful GCM messages.", response.mSuccessCount));
        Log.d(TAG, String.format("%d failed GCM messages.", response.mFailCount));

        // After all the responses have returned, build the app push response model.
        if (mInboundResponses.size() == mOutboundMessages.size()) {

            int totalSuccesses = 0;
            int totalFails = 0;
            boolean hasCriticalError = false;

            List<Recipient> originalRecipients = mOriginalMessage.recipients;
            MessageResult originalMessageResult = new MessageResult();

            // Start the registrationCount before all of the message parts so inbound == outbound
            int overallRegCount = 0;

            // Loop through each message response part.
            for (GoogleResponse googleResponsePart : mInboundResponses) {
                totalSuccesses += googleResponsePart.mSuccessCount;
                totalFails += googleResponsePart.mFailCount;

                // Loop through each response from each registration.
                for (int i = 0; i < googleResponsePart.mResults.size(); i++) {
                    GoogleResponse.ResultData resultData = googleResponsePart.mResults.get(i);
                    String originalRegToken = originalRecipients.get(overallRegCount).recipientId;

                    // A successful message.
                    if (resultData.messageId != null && !resultData.messageId.isEmpty()) {
                        originalMessageResult.addSuccessToRegistration(originalRegToken, resultData.messageId);
                    }

                    // Check for changed registration token.
                    if (resultData.registrationId != null && !resultData.registrationId.isEmpty()) {
                        originalMessageResult.addUpdatedRegistration(originalRegToken, resultData.registrationId);
                    }

                    // Check for errors.
                    if (resultData.error != null && !resultData.error.isEmpty()) {

                        // Check for a critical error in the response.
                        for (GoogleResponse.ResponseError error : GoogleResponse.ResponseError.values()) {

                            // For not registered, add to stale registrations
                            if (error.equals(GoogleResponse.ResponseError.ERROR_NOT_REGISTERED)) {
                                originalMessageResult.addStaleRegistration(originalRegToken);
                            }

                            // For not invalid registration, add to stale registrations
                            if (error.equals(GoogleResponse.ResponseError.ERROR_INVALID_REGISTRATION)) {
                                originalMessageResult.addStaleRegistration(originalRegToken);
                            }

                            if (!hasCriticalError && error.isCritical) {
                                hasCriticalError = true;
                            }
                        }

                        // Add the error for that particular registration
                        originalMessageResult.addErrorToRegistration(originalRegToken, resultData.error);
                    }

                    // Bump the master registration counter for all parts.
                    overallRegCount++;
                }
            }

            // Add the critical flag  into the response if there was one.
            originalMessageResult.setSuccessCount(totalSuccesses);
            originalMessageResult.setFailCount(totalFails);
            originalMessageResult.setHasCriticalErrors(hasCriticalError);
            originalMessageResult.setOriginalMessage(mOriginalMessage);

            // Send back to client.
            if (hasCriticalError) {
                mResponseInterface.messageFailed(originalMessageResult);
            } else {
                mResponseInterface.messageSuccess(originalMessageResult);
            }
        }
    }


    /**
     * Clone a message completely without any registration information
     *
     * @param message message to copy.
     * @return a copied message that is exactly the same but with registration information removed.
     */
    private Message cloneMessage(Message message) {
        if (message != null) {
            Message clonedMessage = new Message();
            clonedMessage.messageId = message.messageId;
            clonedMessage.platformAccount = message.platformAccount;
            clonedMessage.recipients = new ArrayList<>();
            clonedMessage.payloadData = message.payloadData;
            clonedMessage.collapseKey = message.collapseKey;
            clonedMessage.ttl = message.ttl;
            clonedMessage.restrictedPackageName = message.restrictedPackageName;
            clonedMessage.isDryRun = message.isDryRun;
            clonedMessage.isDelayWhileIdle = message.isDelayWhileIdle;
            clonedMessage.priority = message.priority;
            clonedMessage.task = message.task;
            clonedMessage.messageSent = message.messageSent;

            return clonedMessage;
        }
        return null;
    }
}
