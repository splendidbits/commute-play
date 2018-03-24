package dao;

import io.ebean.EbeanServer;
import io.ebean.OrderBy;
import models.devices.Device;
import models.devices.Subscription;
import play.Logger;

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

            if (!devices.isEmpty()) {
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
                    .setOrder(new OrderBy<>("time_registered desc"))
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .where()
                    .eq("deviceId", deviceId)
                    .query()
                    .findList();

            Device device = !devices.isEmpty()
                    ? devices.get(devices.size() -1)
                    : null;

            Logger.debug(device != null
                    ? String.format("Found device with deviceId %s", device.deviceId)
                    : String.format("No device found deviceId %s", deviceId));

            return device;

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
            List<Device> devices = mEbeanServer.find(Device.class)
                    .setOrder(new OrderBy<>("time_registered desc"))
                    .where()
                    .eq("token", staleToken)
                    .query()
                    .findList();

            if (devices.isEmpty()) {
                Logger.debug(String.format("No device found for token %s", staleToken));
                return false;
            }

            mEbeanServer.update(Device.class)
                    .set("token", newToken)
                    .where()
                    .eq("token", staleToken)
                    .update();

        } catch (Exception e) {
            Logger.error("Error persisting updated Device Token", e);
            return false;
        }
        return true;
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

        } catch (Exception e) {
            Logger.error(String.format("Error deleting device for %s.", deviceToken), e);
            return false;
        }
        return true;
    }

    /**
     * Save a device for a device to the database. Will find any previous devices
     * based on device deviceId or device deviceId and delete them first (and their subscription children).
     *
     * @param device device to save.
     * @return success boolean.
     */
    public boolean saveDevice(@Nonnull Device device) {
        if (device.deviceId != null && device.account != null) {

            try {
                List<Device> matchingDevices = mEbeanServer.find(Device.class)
                        .setOrder(new OrderBy<>("time_registered desc"))
                        .fetch("account")
                        .fetch("subscriptions")
                        .fetch("subscriptions.route")
                        .fetch("subscriptions.route.agency")
                        .where()
                        .disjunction()
                        .eq("deviceId", device.deviceId)
                        .eq("token", device.token)
                        .endJunction()
                        .query()
                        .findList();

                for (Device matchingDevice : matchingDevices) {
                    if (matchingDevice.subscriptions != null) {
                        mEbeanServer.deleteAll(matchingDevice.subscriptions);
                    }
                    mEbeanServer.delete(matchingDevice);
                }

                mEbeanServer.insert(device);

            } catch (Exception e) {
                Logger.error(String.format("Error saving device and subscriptions for deviceId: %s.", device.deviceId), e.getMessage());
                return false;
            }

        } else {
            Logger.error("Device must contain a deviceId (UUID) and account.");
            return false;
        }
        return true;
    }

}
