package dao;

import com.avaje.ebean.*;
import models.devices.Device;
import models.devices.Subscription;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;

/**
 * A DAO class for both device device / subscription data.
 */
public class DeviceDao {
    private EbeanServer mEbeanServer;

    @Inject
    public DeviceDao(EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    @Nonnull
    public List<Device> getAccountDevices(@Nonnull String apiKey, int page, Integer agencyId) {
        final int MAX_ROWS = 150;
        List<Device> foundDevices = new ArrayList<>();

        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setReadOnly(true);

        try {
            ExpressionList<Device> devicesQuery = mEbeanServer.createQuery(Device.class)
                    .setOrder(new OrderBy<>("timeRegistered"))
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .setFirstRow(page * MAX_ROWS)
                    .setMaxRows(MAX_ROWS)
                    .where()
                    .eq("account.apiKey", apiKey)
                    .eq("subscriptions.route.agency.id", agencyId != null ? agencyId : 999);

            List<Device> devices = mEbeanServer.findList(devicesQuery.query(), transaction);
            if (devices != null) {
                foundDevices.addAll(devices);
            }

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency model to database: %s.", e.getMessage()));
            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException ERROR:")) {
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }

        return foundDevices;
    }

    /**
     * Fetch a saved a device model.
     *
     * @param deviceId deviceId for which to find device.
     * @return device model.
     */
    @Nullable
    public Device getDevice(@Nonnull String deviceId) {
        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setReadOnly(true);

        try {
            Query<Device> deviceQuery = mEbeanServer.find(Device.class)
                    .fetch("subscriptions")
                    .where()
                    .eq("deviceId", deviceId)
                    .query();

            Device device = mEbeanServer.findUnique(deviceQuery, transaction);

            String logString = device != null
                    ? String.format("Found device with deviceId %s", device.deviceId)
                    : String.format("No device found deviceId %s", deviceId);

            Logger.debug(logString);
            return device;

        } catch (Exception e) {
            Logger.error("Error persisting subscription", e);
            return null;

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }
    }

    /**
     * Update a device token.
     *
     * @param staleToken stale token for device.
     * @param newToken   updated token for device.
     * @return true if the stale device was found by token and updated
     */
    public boolean saveUpdatedToken(@Nonnull String staleToken, @Nullable String newToken) {
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            Query<Device> deviceQuery = mEbeanServer.find(Device.class)
                    .where()
                    .eq("token", staleToken)
                    .query();
            Device device = mEbeanServer.findUnique(deviceQuery, transaction);

            Logger.debug(device != null
                    ? String.format("Found and updating device %1$s with new token %2$s", device.deviceId, newToken)
                    : String.format("No device found for token %s", staleToken));

            if (device != null) {
                device.token = newToken;
                mEbeanServer.save(device, transaction);
                transaction.commit();
                transaction.end();
                return true;
            }

            return false;

        } catch (Exception e) {
            Logger.error("Error persisting subscription", e);
            return false;

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }
    }


    /**
     * Delete a device device, and all route subscriptions.
     *
     * @param deviceToken the device token for the device.
     * @return true if the device was deleted, or false if there was an exception
     * or never existed in the first place.
     */
    public boolean removeDevice(@Nonnull String deviceToken) {
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            Query<Subscription> subscriptionQuery = mEbeanServer.createQuery(Subscription.class)
                    .where()
                    .eq("device.token", deviceToken)
                    .query();

            Query<Device> deviceQuery = mEbeanServer.createQuery(Device.class)
                    .where()
                    .eq("token", deviceToken)
                    .query();

            mEbeanServer.delete(subscriptionQuery, transaction);
            mEbeanServer.delete(deviceQuery, transaction);
            transaction.commit();
            transaction.end();

            Logger.debug(String.format("Removed device %s,", deviceToken));
            return true;

        } catch (Exception e) {
            transaction.rollback();
            Logger.error(String.format("Error deleting device for %s.", deviceToken), e);
            return false;

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }
    }

    /**
     * Save a device for a device to the database. Will find any previous devices
     * based on device deviceId or device deviceId and delete them first (and their subscription children).
     *
     * @param device device to save.
     * @return success boolean.
     */
    public boolean saveDevice(@Nonnull Device device) {
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            Device matchingDevice = null;

            // Build a query depending on if we have a token, and or device identifier.
            if (device.token != null) {
                matchingDevice = mEbeanServer.createQuery(Device.class)
                        .where()
                        .eq("token", device.token)
                        .findUnique();

            } else if (device.deviceId != null) {
                matchingDevice = mEbeanServer.createQuery(Device.class)
                        .where()
                        .eq("deviceId", device.id)
                        .findUnique();
            }

            // Update an existing device if it exists.
            if (matchingDevice != null) {

                // Delete existing subscriptions
                mEbeanServer.find(Subscription.class)
                        .where()
                        .eq("device.id", matchingDevice.id)
                        .delete();

                matchingDevice.deviceId = device.deviceId;
                matchingDevice.token = device.token;
                matchingDevice.account = device.account;
                matchingDevice.appKey = device.appKey;
                matchingDevice.userKey = device.userKey;
                matchingDevice.subscriptions = device.subscriptions;
                mEbeanServer.save(matchingDevice, transaction);

            } else {
                mEbeanServer.insert(device, transaction);
            }

            transaction.commit();
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error saving device device for %s.", device.deviceId), e);
            transaction.rollback();

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }

        return false;
    }

}
