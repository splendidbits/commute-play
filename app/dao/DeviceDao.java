package dao;

import com.avaje.ebean.*;
import models.devices.Device;
import models.devices.Subscription;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;

/**
 * A DAO class for both device device / subscription data.
 */
public class DeviceDao extends BaseDao {

    @Inject
    public DeviceDao(EbeanServer ebeanServer) {
        super(ebeanServer);
    }

    @Nonnull
    public List<Device> getAccountDevices(@Nonnull String apiKey, int agencyId) {
        List<Device> foundDevices = new ArrayList<>();

        Transaction transaction = mEbeanServer.createTransaction();
        int batchSize = 500;
        transaction.setBatchMode(true);
        transaction.setBatchSize(batchSize);
        transaction.setReadOnly(true);

        try {
            ExpressionList<Device> devicesQuery = mEbeanServer.createQuery(Device.class)
                    .setLazyLoadBatchSize(250)
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .where()
                    .eq("account.apiKey", apiKey);

            List<Device> devices = mEbeanServer.findList(devicesQuery.query(), transaction);

            if (devices != null && !devices.isEmpty()) {
                foundDevices.addAll(devices);
            }

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency model to database: %s.", e.getMessage()));

            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);

        } finally {
            transaction.end();
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
                    .setMaxRows(1)
                    .orderBy().desc("id")
                    .fetch("subscriptions")
                    .where()
                    .eq("deviceId", deviceId)
                    .query();

            List<Device> devices = mEbeanServer.findList(deviceQuery, transaction);

            String logString = devices != null && !devices.isEmpty()
                    ? String.format("Found device with deviceId %s", devices.get(0).deviceId)
                    : String.format("No device found deviceId %s", deviceId);

            Logger.debug(logString);
            return devices.get(0);

        } catch (Exception e) {
            Logger.error("Error persisting subscription", e);
            return null;

        } finally {
            transaction.end();
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
        transaction.setReadOnly(false);

        try {
            Query<Device> deviceQuery = mEbeanServer.find(Device.class)
                    .setMaxRows(1)
                    .orderBy().desc("id")
                    .where()
                    .disjunction()
                    .eq("token", staleToken)
                    .eq("token", newToken)
                    .endJunction()
                    .query();

            List<Device> devices = mEbeanServer.findList(deviceQuery, transaction);
            Device device = null;

            if (devices != null && !devices.isEmpty()) {
                device = devices.get(0);
                mEbeanServer.markAsDirty(device);

                device.token = newToken;
                mEbeanServer.save(device, transaction);
                transaction.commit();
                return true;
            }

            Logger.debug(devices != null && !devices.isEmpty()
                    ? String.format("Found and updating device %1$s with new token %2$s", device.deviceId, newToken)
                    : String.format("No device found for token %s", staleToken));

            return false;

        } catch (Exception e) {
            Logger.error("Error persisting updated Device Token", e);
            return false;

        } finally {
            transaction.end();
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
        try {
            mEbeanServer.find(Subscription.class)
                    .fetch("device")
                    .where()
                    .eq("device.token", deviceToken)
                    .delete();

            mEbeanServer.find(Device.class)
                    .where()
                    .eq("token", deviceToken)
                    .delete();

            Logger.debug(String.format("Removed device %s,", deviceToken));
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error deleting device for %s.", deviceToken), e);
            return false;
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
        mEbeanServer.markAsDirty(device);

        try {
            List<Device> matchingDevices;

            // Build a query depending on if we have a token, and or device identifier.
            if (device.deviceId != null) {
                matchingDevices = mEbeanServer.createQuery(Device.class)
                        .orderBy().desc("id")
                        .setMaxRows(1)
                        .where()
                        .eq("deviceId", device.deviceId)
                        .findList();

                // If a device exists. update, otherwise insert.
                if (matchingDevices != null && !matchingDevices.isEmpty()) {
                    Device latestDevice = matchingDevices.get(0);

                    // Delete any current subscriptions
                    mEbeanServer.find(Subscription.class)
                            .where()
                            .eq("device.deviceId", latestDevice.deviceId)
                            .delete();

                    latestDevice.id = device.id;
                    latestDevice.deviceId = device.deviceId;
                    latestDevice.token = device.token;
                    latestDevice.account = device.account;
                    latestDevice.appKey = device.appKey;
                    latestDevice.userKey = device.userKey;
                    latestDevice.subscriptions = device.subscriptions;
                    mEbeanServer.save(matchingDevices, transaction);

                } else {
                    mEbeanServer.insert(device, transaction);
                }

                transaction.commit();
                return true;

            } else {
                Logger.error("Device must contain a deviceId (UUID) to save.");
            }

        } catch (Exception e) {
            Logger.error(String.format("Error saving device and subscriptions for deviceId: %s.", device.deviceId), e);
            transaction.rollback();

        } finally {
            transaction.end();
        }

        return false;
    }

}
