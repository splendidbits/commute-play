package models.app;

import models.taskqueue.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model which contains all successes, minor, device, and critical
 * errors back from any platform provider.
 */
public class MessageResult{
    private int mSuccessCount = 0;
    private int mFailCount = 0;

    private Message mOriginalMessage = null;
    private List<String> mStaleRegistrationIds = new ArrayList<>();
    private List<String> mRegistrationsIdsToRetry = new ArrayList<>();
    private Map<String, String> mUpdatedRegistrationsMap = new HashMap<>();
    private Map<String, String> mSuccessResultsMap = new HashMap<>();
    private Map<String, String> mErrorResultsMap = new HashMap<>();
    private boolean mHasCriticalErrors;

    public MessageResult() {
    }

    public void setFailCount(int failCount) {
        mFailCount = failCount;
    }

    public void setSuccessCount(int successCount) {
        mSuccessCount = successCount;
    }

    public boolean hasCriticalErrors() {
        return mHasCriticalErrors;
    }

    public void setHasCriticalErrors(boolean hasCriticalErrors) {
        mHasCriticalErrors = hasCriticalErrors;
    }

    public int getSuccessCount() {
        return mSuccessCount;
    }

    public int getFailCount() {
        return mFailCount;
    }

    public Message getOriginalMessage() {
        return mOriginalMessage;
    }

    public void setOriginalMessage(Message originalMessage) {
        mOriginalMessage = originalMessage;
    }

    public List<String> getStaleRegistationIds() {
        return mStaleRegistrationIds;
    }

    public List<String> getRegistrationsIdsToRetry() {
        return mRegistrationsIdsToRetry;
    }

    public Map<String, String> getUpdatedRegistrationsMap() {
        return mUpdatedRegistrationsMap;
    }

    public void addStaleRegistration(String staleRegistrationId) {
        mStaleRegistrationIds.add(staleRegistrationId);
    }

    public void addUpdatedRegistration(String oldRegistrationId, String newRegistrationId) {
        mUpdatedRegistrationsMap.put(oldRegistrationId, newRegistrationId);
    }

    public void addErrorToRegistration(String registrationId, String result) {
        mErrorResultsMap.put(registrationId, result);
    }

    public void addSuccessToRegistration(String registrationId, String messageId) {
        mSuccessResultsMap.put(registrationId, messageId);
    }
}
