package services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import helpers.GcmMessageSerializer;
import interfaces.MessageResponseListener;
import main.Log;
import models.accounts.PlatformAccount;
import models.app.GoogleResponse;
import models.app.MessageResult;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
     * @param platformAccount  The platform account for which to send the message with.
     * @param responseListener The response listener.
     */
    @SuppressWarnings("Convert2Lambda")
    public void dispatchMessageAsync(@Nonnull Message message, @Nonnull MessageResponseListener responseListener,
                                     @Nonnull PlatformAccount platformAccount) {

        // Return error on  no recipients.
        if (message.recipients == null || message.recipients.isEmpty()) {
            responseListener.messageFailed(message, MessageResponseListener.HardFailCause.MISSING_RECIPIENTS);
            return;
        }

        List<Message> messageParts = organiseMessageParts(message);
        List<GoogleResponse> messagePartResponses = new ArrayList<>();

        // Send each message part one by one asynchronously, and add the result into a list.
        for (Message messagePart : messageParts) {

            try {
                WSResponse partResponse = sendMessageRequest(messagePart, platformAccount);
                if (partResponse != null) {
                    GoogleResponse partGoogleResponse = parseCallResponse(partResponse);
                    messagePartResponses.add(partGoogleResponse);
                }

            } catch (GoogleCallException callException) {
                mLog.e(TAG, "Error while making Google GCM request call", callException);

                if (callException.mStatusCode > -1) {
                    switch (callException.mStatusCode) {
                        case 200:
                            break;

                        case 400:
                            responseListener.messageFailed(message, MessageResponseListener.HardFailCause.GCM_MESSAGE_JSON_ERROR);
                            return;

                        case 401:
                            responseListener.messageFailed(message, MessageResponseListener.HardFailCause.AUTH_ERROR);
                            return;

                        default:
                            responseListener.messageFailed(message, MessageResponseListener.HardFailCause.UNKNOWN_ERROR);
                            break;
                    }
                }
            }
        }

        // If there were some message part results, combine them into master result and send to client.
        if (!messagePartResponses.isEmpty()) {
            MessageResult clientResponse = combineMessageResponses(message, messagePartResponses);
            responseListener.messageResult(clientResponse);
        }
    }

    /**
     * Internally send a message using the GCM protocol to google. If a message contains
     * more than 1000 registration ids, it'll split that into multiples messages.
     *
     * @param originalMessage the client message sent to the dispatcher.
     * @return A list of organised parts.
     */
    private List<Message> organiseMessageParts(@Nonnull Message originalMessage) {
        List<Message> returnedMessageParts = new ArrayList<>();
        List<Recipient> allRecipients = originalMessage.recipients;

        Message messagePart = cloneMessage(originalMessage);
        int messageRegistrations = 0;

        // Add each registration_id to the message in batches of 1000.
        if (allRecipients != null && !allRecipients.isEmpty()) {
            for (int regCount = 0; regCount < allRecipients.size(); regCount++) {

                // If there's ~1000 registrations, add the message to the pending messages
                if (messageRegistrations == 1000) {
                    returnedMessageParts.add(messagePart);

                    messagePart = cloneMessage(originalMessage);
                    messageRegistrations = 0;
                }

                // Add the registration to the new message.
                messagePart.recipients.add(allRecipients.get(regCount));
                messageRegistrations++;

                // Final registration in iteration? Start processing the batches
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
     * @param messagePart     the message or message part to send.
     * @param platformAccount the platform account for which to send the messgage from.
     * @return WSResponse google request response.
     */
    @Nullable
    private WSResponse sendMessageRequest(@Nonnull Message messagePart, @Nonnull PlatformAccount platformAccount) {
        if (messagePart.platformAccount.platform != null &&
                messagePart.platformAccount.platform.endpointUrl != null) {

            String jsonBody = new GsonBuilder()
                    .registerTypeAdapter(Message.class, new GcmMessageSerializer())
                    .create()
                    .toJson(messagePart);

            WSResponse response = (WSResponse) mWsClient
                    .url(messagePart.platformAccount.platform.endpointUrl)
                    .setContentType("application/json")
                    .setHeader("Authorization", String.format("key=%s", platformAccount.authToken))
                    .setRequestTimeout(GCM_REQUEST_TIMEOUT)
                    .setFollowRedirects(true)
                    .setBody(jsonBody)
                    .execute("post");

            return response;
        }
        return null;
    }

    /**
     * Parse an incoming response back from a GCM send action.
     *
     * @param callResponse PlatformMessageResult The WSResponse back from google which
     *                     should contain a json success / fail map.
     * @return MessageResult result from google for gcm message.
     */
    @Nonnull
    private GoogleResponse parseCallResponse(@Nonnull WSResponse callResponse) throws GoogleCallException {
        GoogleResponse parsedGoogleResponse = null;

        // Return any hard errors immediately.
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
     * @param responses a list of received Google Responses.
     * @return The master GoogleResponse for the original message send back to the client.
     */
    @Nonnull
    private MessageResult combineMessageResponses(@Nonnull Message message, @Nonnull List<GoogleResponse> responses) {
        MessageResult messageResult = new MessageResult();

        int totalSuccesses = 0;
        int totalFails = 0;
        boolean hasCriticalError = false;

        // Start the registrationCount before all of the message parts so inbound == outbound
        int runningRegCounter = 0;

        // Loop through each message response part.
        for (GoogleResponse googleResponse : responses) {
            totalSuccesses += googleResponse.mSuccessCount;
            totalFails += googleResponse.mFailCount;

            // Loop through each response from each registration.
            for (int i = 0; i < googleResponse.mResults.size(); i++) {
                GoogleResponse.ResultData resultData = googleResponse.mResults.get(i);
                String originalRegToken = message.recipients.get(runningRegCounter).recipientId;

                // A successful message.
                if (resultData.messageId != null && !resultData.messageId.isEmpty()) {
                    messageResult.addSuccessToRegistration(originalRegToken, resultData.messageId);
                }

                // Check for changed registration token.
                if (resultData.registrationId != null && !resultData.registrationId.isEmpty()) {
                    messageResult.addUpdatedRegistration(originalRegToken, resultData.registrationId);
                }

                // Check for errors.
                if (resultData.error != null && !resultData.error.isEmpty()) {

                    // Check for a critical error in the response.
                    for (GoogleResponse.ResponseError error : GoogleResponse.ResponseError.values()) {

                        // For not registered, add to stale registrations
                        if (error.equals(GoogleResponse.ResponseError.ERROR_NOT_REGISTERED)) {
                            messageResult.addStaleRegistration(originalRegToken);
                        }

                        // For not invalid registration, add to stale registrations
                        if (error.equals(GoogleResponse.ResponseError.ERROR_INVALID_REGISTRATION)) {
                            messageResult.addStaleRegistration(originalRegToken);
                        }

                        if (!hasCriticalError && error.isCritical) {
                            hasCriticalError = true;
                        }
                    }

                    // Add the error for that particular registration
                    messageResult.addErrorToRegistration(originalRegToken, resultData.error);
                }

                // Bump the master registration counter for all parts.
                runningRegCounter++;
            }
        }

        // Add the critical flag  into the response if there was one.
        messageResult.setSuccessCount(totalSuccesses);
        messageResult.setFailCount(totalFails);
        messageResult.setHasCriticalErrors(hasCriticalError);
        messageResult.setOriginalMessage(message);

        return messageResult;
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
            clonedMessage.sentTime = message.sentTime;

            return clonedMessage;
        }
        return null;
    }

    /**
     * Thrown on a hard, breaking http call error such as 404.
     */
    private class GoogleCallException extends Exception {
        private int mStatusCode = -1;
        private String mErrorMessage;

        public GoogleCallException(String errorMessage, int statusCode) {
            mErrorMessage = errorMessage;
            mStatusCode = statusCode;
        }
    }

}
