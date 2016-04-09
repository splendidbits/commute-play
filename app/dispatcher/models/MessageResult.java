package dispatcher.models;

import models.taskqueue.Recipient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model which contains all successes, minor, device, and critical
 * errors back from any platform provider.
 */
public class MessageResult{
    private List<Recipient> mRecipientsToRetry = new ArrayList<>();
    private List<Recipient> mSuccessfulRecipients = new ArrayList<>();
    private List<Recipient> mStaleRecipients = new ArrayList<>();

    private Map<Recipient, Recipient> mUpdatedRegistrationsMap = new HashMap<>();
    private Map<Recipient, String> mErrorResultsMap = new HashMap<>();

    public MessageResult() {
    }

    public List<Recipient> getRecipientsToRetry() {
        return mRecipientsToRetry;
    }

    public List<Recipient> getSuccessfulRecipients() {
        return mSuccessfulRecipients;
    }

    public List<Recipient> getStaleRecipients() {
        return mStaleRecipients;
    }

    public Map<Recipient, Recipient> getUpdatedRegistrations() {
        return mUpdatedRegistrationsMap;
    }

    public Map<Recipient, String> getRecipientErrors() {
        return mErrorResultsMap;
    }

    public void addSuccessfulRecipient(Recipient recipient) {
        mSuccessfulRecipients.add(recipient);
    }

    public void addStaleRecipient(Recipient staleRecipient) {
        mStaleRecipients.add(staleRecipient);
    }

    public void addUpdatedRecipient(Recipient oldRecipient, Recipient newRecipient) {
        mUpdatedRegistrationsMap.put(oldRecipient, newRecipient);
    }

    public void addErrorToRecipient(Recipient recipient, String cause) {
        mErrorResultsMap.put(recipient, cause);
    }
}
