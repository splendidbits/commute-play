package pushservices.models.app;

import pushservices.models.database.Recipient;
import pushservices.models.database.RecipientFailure;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model which contains all successes, minor, device, and critical
 * errors back from any platform provider.
 */
public class MessageResult {
    private List<Recipient> mRecipientsToRetry = new ArrayList<>();
    private List<Recipient> mSuccessfulRecipients = new ArrayList<>();

    private List<UpdatedRegistration> mUpdatedRegistrations = new ArrayList<>();
    private Map<Recipient, RecipientFailure> mErrorResultsMap = new HashMap<>();

    public MessageResult() {
    }

    public List<Recipient> getRecipientsToRetry() {
        return mRecipientsToRetry;
    }

    public List<Recipient> getSuccessfulRecipients() {
        return mSuccessfulRecipients;
    }

    public Map<Recipient, RecipientFailure> getFailedRecipients() {
        return mErrorResultsMap;
    }

    public List<UpdatedRegistration> getUpdatedRegistrations() {
        return mUpdatedRegistrations;
    }

    public Map<Recipient, RecipientFailure> getRecipientErrors() {
        return mErrorResultsMap;
    }

    public void addSuccessfulRecipient(@Nonnull Recipient recipient) {
        mSuccessfulRecipients.add(recipient);
    }

    public void addFailedRecipients(@Nonnull Recipient recipient, @Nonnull RecipientFailure error) {
        mErrorResultsMap.put(recipient, error);
    }

    public void addFailedRecipients(@Nonnull List<Recipient> recipients, @Nonnull RecipientFailure error) {
        for (Recipient recipient : recipients) {
            mErrorResultsMap.put(recipient, error);
        }
    }

    public void addUpdatedRegistration(@Nonnull Recipient staleRecipient, @Nonnull Recipient updatedRecipient) {
        UpdatedRegistration updatedRegistration = new UpdatedRegistration(staleRecipient, updatedRecipient);
        mUpdatedRegistrations.add(updatedRegistration);
    }
}
