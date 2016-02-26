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
            mEbeanServer.save(subscription);
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
     * @param newReg registration to save.
     * @return success boolean.
     */
    public boolean addRegistration(@Nonnull Registration newReg) {
        if (newReg.registrationId != null && newReg.deviceId != null) {
            Registration newRegistration = new Registration(newReg.deviceId, newReg.registrationId);

            // Build a query depending on if we have a token, and or device identifier.
            Registration existingRegistration = mEbeanServer.createQuery(Registration.class)
                    .where()
                    .eq("registration_id", newReg.registrationId)
                    .findUnique();
            try {
                mEbeanServer.beginTransaction();

                // Delete an existing registration if it exists.
                if (existingRegistration != null) {
                    mEbeanServer.delete(Registration.class,
                            existingRegistration.deviceId,
                            mEbeanServer.currentTransaction());
                }

                // Save the new registration and commit.
                mEbeanServer.save(newRegistration);
                mEbeanServer.commitTransaction();
                return true;

            } catch (Exception e) {
                mEbeanServer.rollbackTransaction();

            } finally {
                mEbeanServer.endTransaction();
            }
        }
        return false;
    }

}
