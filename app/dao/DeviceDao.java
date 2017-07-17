package dao;

import io.ebean.EbeanServer;
import io.ebean.Junction;
import models.accounts.Account;
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
        int batchSize = 500;

        try {
            List<Device> devices = mEbeanServer.createQuery(Device.class)
                    .setLazyLoadBatchSize(batchSize)
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .where()
                    .eq("account.apiKey", apiKey)
                    .findList();

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
        try {
            List<Device> devices = mEbeanServer.find(Device.class)
                    .setMaxRows(1)
                    .orderBy().desc("id")
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .where()
                    .eq("deviceId", deviceId)
                    .query()
                    .findList();

            String logString = devices != null && !devices.isEmpty()
                    ? String.format("Found device with deviceId %s", devices.get(0).deviceId)
                    : String.format("No device found deviceId %s", deviceId);

            if (devices != null && !devices.isEmpty()) {
                Logger.debug(logString);
                return devices.get(0);
            }

        } catch (Exception e) {
            Logger.error("Error persisting subscription", e);
        }
        return null;
    }

    /**
     * Update a device token.
     *
     * @param staleToken stale token for device.
     * @param newToken   updated token for device.
     * @return true if the stale device was found by token and updated
     */
    public boolean saveUpdatedToken(@Nonnull String staleToken, @Nullable String newToken) {
        try {
            Device device = mEbeanServer.find(Device.class)
                    .fetch("account")
                    .setMaxRows(1)
                    .orderBy().desc("id")
                    .where()
                    .disjunction()
                    .eq("token", staleToken)
                    .endJunction()
                    .findUnique();

            if (device != null && device.id != null) {
                device.token = newToken;
                mEbeanServer.update(device);
                return true;
            }

            Logger.debug(String.format("No device found for token %s", staleToken));

        } catch (Exception e) {
            Logger.error("Error persisting updated Device Token", e);
        }
        return false;
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
                    .fetch("subscriptions")
                    .where()
                    .eq("token", deviceToken)
                    .delete();

            Logger.debug(String.format("Removed device %s,", deviceToken));
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error deleting device for %s.", deviceToken), e);
        }
        return false;
    }

    /**
     * Save a device for a device to the database. Will find any previous devices
     * based on device deviceId or device deviceId and delete them first (and their subscription children).
     *
     * @param device device to save.
     * @return success boolean.
     */
    public boolean saveDevice(@Nonnull Device device) {

        try {
            if (device.account != null) {
                Junction<Account> accountSearch = mEbeanServer.createQuery(Account.class)
                        .where()
                        .disjunction();

                if (device.account.id != null) {
                    accountSearch.eq("id", device.account.id);

                } else if (device.account.apiKey != null) {
                    accountSearch.eq("api_key", device.account.apiKey);
                }

                Account savedAccount = accountSearch
                        .endJunction()
                        .findUnique();

                if (savedAccount == null) {
                    mEbeanServer.save(device.account);
                }
            }

            // Build a query depending on if we have a token, and or device identifier.
            if (device.deviceId != null) {
                List<Device> matchingDevices = mEbeanServer.find(Device.class)
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
                    mEbeanServer.save(latestDevice);

                } else {
                    mEbeanServer.insert(device);
                }

                return true;

            } else {
                Logger.error("Device must contain a deviceId (UUID) to save.");
            }

        } catch (Exception e) {
            Logger.error(String.format("Error saving device and subscriptions for deviceId: %s.", device.deviceId), e);
        }

        return false;
    }

}
