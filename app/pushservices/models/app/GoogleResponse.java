package pushservices.models.app;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response model from Google GCM endpoint (for response status 200).
 */
public class GoogleResponse {

    @SerializedName("multicast_id")
    public String mMessageId;

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
