package models.app;

import models.registrations.Registration;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class GoogleResponse {
    private Map<Registration, GoogleResponse> mMessageResponses = new HashMap<>();

    public void addResponse(@Nonnull Registration registration, @Nonnull GoogleResponse response) {
        mMessageResponses.put(registration, response);
    }

    public Map<Registration, GoogleResponse> getMessageResponses() {
        return mMessageResponses;
    }

    public enum GoogleResponseInfo {
        OK("OK"),
        REGISTRATION_ID_UPDATED("Registration identifier has updated for device."),
        ERROR_INVALID_REGISTRATION("The device registration was not registered with Google."),
        ERROR_NOT_REGISTERED("The device registration was not registered with Google."),
        ERROR_UNAVAILABLE("GCM has requested a try again after exponential backoff");

        public String value;
        GoogleResponseInfo(String value) {
            this.value = value;
        }
    }

}