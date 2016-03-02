package models.app;

import models.registrations.Registration;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GoogleMessage {
    private List<String> mRegistrations = new ArrayList<>();
    private HashMap<String, String> mDataTokenMap = new HashMap<>();
    private String mCollapseKey;

    public GoogleMessage(@Nonnull List<Registration> registrations) {
        for (Registration registration : registrations) {
            mRegistrations.add(registration.registrationId);
        }
    }

    public GoogleMessage(@Nonnull String registrationId) {
        mRegistrations.add(registrationId);
    }

    public GoogleMessage(@Nonnull List<String> registrations, @Nonnull HashMap<String, String> dataTokenMap) {
        mRegistrations = registrations;
        mDataTokenMap = dataTokenMap;
    }

    public void addData(@Nonnull String token, @Nonnull String data) {
        mDataTokenMap.put(token, data);
    }

    public void setCollapseKey(String collapseKey) {
        mCollapseKey = collapseKey;
    }

    public void addRegistrationId(@Nonnull String registrationId) {
        mRegistrations.add(registrationId);
    }

    public String getCollapseKey() {
        return mCollapseKey;
    }

    public HashMap<String, String> getDataTokenMap() {
        return mDataTokenMap;
    }

    public List<String> getRegistrations() {
        return mRegistrations;
    }
}
