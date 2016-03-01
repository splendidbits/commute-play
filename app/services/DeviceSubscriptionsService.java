package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import main.Constants;
import main.Log;
import models.registrations.Registration;
import models.registrations.Subscription;

import javax.annotation.Nonnull;

public class DeviceSubscriptionsService {
    private EbeanServer mEbeanServer;

    public DeviceSubscriptionsService() {
        try {
            mEbeanServer = Ebean.getServer(Constants.COMMUTE_GCM_DB_SERVER);
        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
        }
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
            Log.e("Error persisting subscription", e);

        } finally {
            mEbeanServer.endTransaction();
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
        if (newRegistration.registrationId != null && newRegistration.deviceId != null) {

            // Build a query depending on if we have a token, and or device identifier.
            Registration existingDevice = mEbeanServer.createQuery(Registration.class)
                    .where()
                    .eq("registration_id", newRegistration.registrationId)
                    .findUnique();

            try {
                mEbeanServer.createTransaction();

                // Update an existing registration if it exists.
                if (existingDevice != null) {
                    mEbeanServer.refresh(newRegistration);
                    mEbeanServer.update(newRegistration);

                } else {
                    mEbeanServer.save(newRegistration);
                }

                // Commit the new registration transaction.
                mEbeanServer.commitTransaction();
                return true;

            } catch (Exception e) {
                mEbeanServer.rollbackTransaction();
                Log.e(String.format("Error saving device registration for %s. Rolling back.",
                        newRegistration.deviceId), e);

            } finally {
                mEbeanServer.endTransaction();
            }
        }
        return false;
    }

}
