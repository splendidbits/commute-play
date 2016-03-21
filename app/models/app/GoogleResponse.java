package models.app;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response model from Google GCM endpoint (for response status 200).
 */
public class GoogleResponse {

    public enum ResponseError {
        ERROR_MISSING_REG_TOKEN("MissingRegistration", false),
        ERROR_INVALID_REGISTRATION("InvalidRegistration", false),
        ERROR_NOT_REGISTERED("NotRegistered", false),
        ERROR_INVALID_PACKAGE_NAME("InvalidPackageName", true),
        ERROR_MISMATCHED_SENDER_ID("MismatchSenderId", true),
        ERROR_MESSAGE_TO_BIG("MessageTooBig", true),
        ERROR_INVALID_DATA("InvalidDataKey", true),
        ERROR_INVALID_TTL("InvalidTtl", true),
        ERROR_EXCEEDED_MESSAGE_LIMIT("DeviceMessageRate Exceeded", true);

        public String friendlyError = null;
        public boolean isCritical = false;
        ResponseError(String value, boolean critical) {
            friendlyError = value;
            isCritical = critical;
        }
    }

    @SerializedName("multicast_id")
    public String mGcmMessageId;

    @SerializedName("success")
    public int mSuccessCount;

    @SerializedName("failure")
    public int mFailCount;

    @SerializedName("canonical_ids")
    public int mCanonicalIdCount;

    @SerializedName("results")
    public List<ResultData> mResults;

    public class ResultData {
        @SerializedName("message_id")
        public String messageId;

        @SerializedName("registration_id")
        public String registrationId;

        @SerializedName("error")
        public String error;
    }
}
