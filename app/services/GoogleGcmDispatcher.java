package services;

import com.google.gson.Gson;
import main.Constants;
import main.Log;
import models.app.GoogleMessage;
import models.app.GoogleResponse;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends GCM alerts to a batch of given tokens and data.
 * <p>
 * - This class just deals with GCM concepts:
 * - A list of registration_ids
 * - A collapse_key
 * - The keys and string data to send.
 */
public class GoogleGcmDispatcher {
    private String mServerId;

    private Map<String, GoogleResponse> mMessageResponses = new HashMap<>();
    private Map<String, String> mRegistrationChanges = new HashMap<>();
    private Map<String, String> mMessageRetries = new HashMap<>();

    public enum CommuteDispatchError {
        FAIL_SERVER_NOT_FOUND("OK"),
        FAIL_BAD_APP_KEY("Registration identifier has updated for device."),
        FAIL_OTHER("The GCM Dispatcher failed for an unknown reason. Check logs.");

        public String value;

        CommuteDispatchError(String value) {
            this.value = value;
        }
    }

    public GoogleGcmDispatcher(String serverId) {
        mServerId = serverId;
    }

    /**
     * Send a message to Google.
     *
     * @param messageBundle     The message bundle.
     * @param responseInterface Response interface.
     */
    public void sendGcmMessage(@Nonnull GoogleMessage messageBundle,
                               @Nonnull GoogleResponseInterface responseInterface) {
        WSResponse gcmResponse;
        try {
            WSRequest request = WS.url(Constants.GOOGLE_GCM_URL);
            request.setContentType("application/json");

            request.get().map((response) -> {
                if (response.getStatus() == 200) {
                    Gson gsonDeserializer = new Gson();
                    gsonDeserializer.toJson(response.asJson());

                } else {
                    responseInterface.messageRequestFailed(CommuteDispatchError.FAIL_SERVER_NOT_FOUND);
                    Log.e("Google GCM message dispatch failed with error " + response.getStatus());
                }
                return response;
            });

        } catch (Exception exception) {
            Log.e("Commute GCM Dispatch error.", exception);
        }
    }


    /**
     * Interface callback to allow clients to know the state of a particular
     * outbound Google GCM message.
     */
    public interface GoogleResponseInterface {
        void messageRequestSuccess(GoogleResponse googleResponse);

        void messageRequestFailed(CommuteDispatchError commuteDispatchError);
    }
}
