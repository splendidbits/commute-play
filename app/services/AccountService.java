package services;

import com.avaje.ebean.EbeanServer;
import main.Constants;
import main.Log;
import models.accounts.Account;
import models.accounts.Platform;
import models.alerts.Route;
import models.registrations.Registration;
import models.registrations.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;

/**
 * A DAO class for both device registration / subscription actions and
 * platform + API account functions.
 * <p>
 * TODO: Split this up into two separate services.
 */
public class AccountService {
    private static final String TAG = AccountService.class.getSimpleName();

    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public AccountService(EbeanServer ebeanServer, Log log) {
        mEbeanServer = ebeanServer;
        mLog = log;
    }

    /**
     * Get a API service Account.
     *
     * @param email registered email address for account.
     * @return an Account object, null if not found.
     */
    @Nullable
    public Account getAccountByEmail(@Nonnull String email) {
        // Build a query depending on if we have a api key, and or registered email.
        return mEbeanServer.createQuery(Account.class)
                .fetch("platformAccounts")
                .fetch("platformAccounts.platform")
                .where()
                .eq("email", email)
                .findUnique();
    }

    /**
     * Get a API service Account.
     *
     * @param apiKey api key for which account was assigned.
     * @return an Account object, null if not found.
     */
    @Nullable
    public Account getAccountByApi(@Nonnull String apiKey) {
        // Build a query depending on if we have a api key, and or registered email.
        return mEbeanServer.createQuery(Account.class)
                .where()
                .eq("api_key", apiKey)
                .findUnique();
    }

    /**
     * Get a device registration object.
     *
     * @param deviceId device id for which to find registration.
     * @return complete registration.
     */
    public Registration getRegistration(@Nonnull String deviceId) {
        if (!deviceId.isEmpty()) {
            return Registration.find.byId(deviceId);
        }
        return null;
    }

    /**
     * Get a platform object for name, such as GCM or APNS.
     *
     * @param platformName name of platform.
     * @return Platform. null if not found
     */
    @Nullable
    public Platform getPlatform(@Nonnull String platformName) {
        if (!platformName.isEmpty()) {
            return Platform.find.byId(platformName.toLowerCase());
        }
        return null;
    }

    /**
     * Get a list of subscriber device registrations for a given route.
     *
     * @param route properly formed route.
     * @return list of subscribers.
     */
    @Nullable
    public List<Account> getRegistrationAccounts(@Nonnull String platformName, int agencyId, @Nonnull Route route) {
        return mEbeanServer.find(Account.class)
                .fetch("registrations")
                .fetch("platformAccounts")
                .fetch("platformAccounts.platform")
                .fetch("registrations.subscriptions")
                .where()
                .eq("platformAccounts.platform.platformName", platformName)
                .where()
                .eq("registrations.subscriptions.routes.routeId", route.routeId)
                .where()
                .eq("registrations.subscriptions.routes.agency.agencyId", agencyId)
                .findList();
    }

    /**
     * Save a device - subscription to the datastore.
     *
     * @param subscription subscription to persist.
     * @return boolean success.
     */
    public boolean addSubscription(@Nonnull Subscription subscription) {
        try {
            // Build a query depending on if we have a token, and or device identifier.
            Subscription existingSubscription = mEbeanServer.find(Subscription.class)
                    .fetch("registration")
                    .where()
                    .eq("registration.deviceId", subscription.registration.deviceId)
                    .findUnique();

            if (existingSubscription != null) {
                mEbeanServer.deleteManyToManyAssociations(existingSubscription, "routes");
                existingSubscription.routes = subscription.routes;
                mEbeanServer.save(existingSubscription);

            } else {
                mEbeanServer.save(subscription);
            }
            return true;

        } catch (Exception e) {
            mLog.e(TAG, "Error persisting subscription", e);

        } finally {
            mEbeanServer.endTransaction();
        }
        return false;
    }



    /**
     * Delete a device registration.
     * @param registrationToken the registration token for the device.
     *
     * @return true or false depending on if the registration was deleted.
     */
    public boolean deleteRegistration(@Nonnull String registrationToken) {
        // Build a query depending on if we have a token, and or device identifier.
        Registration existingRegistration = mEbeanServer.createQuery(Registration.class)
                .where()
                .eq("registration_token", registrationToken)
                .findUnique();

            try {
                // Update an existing registration if it exists.
                if (existingRegistration != null) {
                    mEbeanServer.beginTransaction();
                    mEbeanServer.delete(existingRegistration);
                }
                mEbeanServer.commitTransaction();
                return true;

            } catch (Exception e) {
                mLog.e(TAG, String.format("Error deleting registration for %s.", registrationToken), e);
                mEbeanServer.rollbackTransaction();
            }
        return false;
    }

    /**
     * Save a registration for a device to the database. Will find any previous registrations
     * based on registration id or device id and delete them first (and their subscription decendents).
     *
     * @param newRegistration registration to save.
     * @return success boolean.
     */
    public boolean addRegistration(@Nonnull Registration newRegistration) {
        if (newRegistration.registrationToken != null || newRegistration.deviceId != null) {

            // Build a query depending on if we have a token, and or device identifier.
            Registration existingDevice = mEbeanServer.createQuery(Registration.class)
                    .where()
                    .disjunction()
                    .eq("registration_token", newRegistration.registrationToken)
                    .eq("device_id", newRegistration.deviceId)
                    .endJunction()
                    .findUnique();

            mEbeanServer.beginTransaction();

            try {
                // Update an existing registration if it exists.
                if (existingDevice != null) {
                    existingDevice.deviceId = newRegistration.deviceId;
                    existingDevice.registrationToken = newRegistration.registrationToken;
                    existingDevice.timeRegistered = Calendar.getInstance(Constants.DEFAULT_TIMEZONE);
                    mEbeanServer.update(existingDevice);

                } else {
                    mEbeanServer.insert(newRegistration);
                }
                mEbeanServer.commitTransaction();

            } catch (Exception e) {
                mLog.e(TAG, String.format("Error saving device registration for %s. Rolling back.",
                        newRegistration.deviceId), e);
                mEbeanServer.rollbackTransaction();
                return false;
            }
            return true;
        }
        return false;
    }

}
