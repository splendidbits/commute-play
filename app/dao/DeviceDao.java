package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Transaction;
import models.devices.Device;
import models.devices.Subscription;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
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
    public List<Device> getAccountDevices(@Nonnull String apiKey, @Nullable Integer agencyId) {
        List<Device> foundDevices = new ArrayList<>();

        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setBatchMode(true);
        transaction.setBatchSize(250);
        transaction.setReadOnly(true);

        try {
            ExpressionList<Device> devicesQuery = mEbeanServer.createQuery(Device.class)
                    .setOrder(new OrderBy<>("timeRegistered"))
                    .fetch("account")
                    .fetch("subscriptions")
                    .fetch("subscriptions.route")
                    .fetch("subscriptions.route.agency")
                    .where()
                    .eq("account.apiKey", apiKey);

            if (agencyId != null) {
                devicesQuery.eq("subscriptions.route.agency.id", agencyId);
            }

            List<Device> devices = mEbeanServer.findList(devicesQuery.query(), transaction);
            if (devices != null) {
                foundDevices.addAll(devices);
            }

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency model to database: %s.", e.getMessage()));

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
        Device latestDevice = null;

        try {
            latestDevice = mEbeanServer.find(Device.class)
                    .fetch("subscriptions")
                    .where()
                    .eq("deviceId", deviceId)
                    .orderBy("timeRegistered desc")
                    .setMaxRows(1)
                    .findUnique();

            // Delete older devices matching the same deviceId. This is to cleanup a previous bug,
            if (latestDevice != null) {
                List<Device> olderDevices = mEbeanServer.find(Device.class)
                        .fetch("subscriptions")
                        .where()
                        .conjunction()
                        .eq("deviceId", deviceId)
                        .ne("id", latestDevice.id)
                        .endJunction()
                        .findList();

                mEbeanServer.deleteAll(olderDevices, transaction);
                transaction.commit();
            }

            Logger.debug(latestDevice != null
                    ? String.format("Found device with deviceId %s", latestDevice.deviceId)
                    : String.format("No device found deviceId %s", deviceId));

        } catch (Exception e) {
            Logger.error(String.format("Error getting device for deviceId %s: %s", deviceId, e.getMessage()), e);

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
        }
        return latestDevice;
    }

    /**
     * Update a device token.
     *
     * @param staleToken stale token for device.
     * @param newToken   updated token for device.
     * @return true if the stale device was found by token and updated
     */
    public boolean saveUpdatedToken(@Nonnull String staleToken, @Nonnull String newToken) {
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            Device device = mEbeanServer.find(Device.class)
                    .where()
                    .eq("token", staleToken)
                    .setMaxRows(1)
                    .findUnique();

            if (device != null) {
                device.markAsDirty();
                device.token = newToken;
                mEbeanServer.save(device);

                transaction.commit();
                Logger.debug(String.format("Updated Device with new token %s", newToken));

            } else {
                Logger.warn(String.format("No Device with stale token %s found to update", staleToken));
            }

            return true;

        } catch (Exception e) {
            Logger.error("Error persisting updated Device Token", e);

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
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
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            List<Subscription> deviceSubscriptions = mEbeanServer.find(Subscription.class)
                    .fetch("device")
                    .where()
                    .eq("device.token", deviceToken)
                    .findList();

            List<Device> devices = mEbeanServer.find(Device.class)
                    .where()
                    .eq("token", deviceToken)
                    .findList();

            mEbeanServer.deleteAll(deviceSubscriptions, transaction);
            mEbeanServer.deleteAll(devices, transaction);

            transaction.commit();
            Logger.debug(String.format("Removed devices and device subscriptions for token %s", deviceToken));
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error deleting device for %s.", deviceToken), e);

        } finally {
            if (transaction.isActive()) {
                transaction.end();
            }
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
        if (device.deviceId != null && device.account != null) {
            Transaction transaction = mEbeanServer.createTransaction();
            device.markAsDirty();

            try {
                List<Device> devices = mEbeanServer.find(Device.class)
                        .where()
                        .eq("deviceId", device.deviceId)
                        .findList();

                List<Subscription> deviceSubscriptions = mEbeanServer.find(Subscription.class)
                        .fetch("device")
                        .where()
                        .eq("device.deviceId", device.deviceId)
                        .findList();

                mEbeanServer.deleteAll(deviceSubscriptions, transaction);
                mEbeanServer.deleteAll(devices, transaction);
                mEbeanServer.insert(device, transaction);

                transaction.commit();
                return true;

            } catch (Exception e) {
                Logger.error(String.format("Error saving device and subscriptions for deviceId: %s.", device.deviceId), e);
                transaction.rollback();

            } finally {
                if (transaction.isActive()) {
                    transaction.end();
                }
            }
        }

        return false;
    }

}
