package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import models.devices.Device;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A DAO class for both device device / subscription data.
 */
public class DeviceDao {
    private EbeanServer mEbeanServer;

    @Inject
    public DeviceDao(EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
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
     * @param staleToken stale token for device.
     * @param updatedToken updated token for device.
     *
     * @return true if the stale device was found by token and updated
     */
    public boolean saveUpdatedToken(@Nonnull String staleToken, @Nullable String updatedToken) {
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            Query<Device> deviceQuery = mEbeanServer.find(Device.class)
                    .where()
                    .eq("token", staleToken)
                    .query();

            Device device = mEbeanServer.findUnique(deviceQuery, transaction);

            Logger.debug(device != null
                    ? String.format("Found and updated device with new token %s", updatedToken)
                    : String.format("No device found for token %s", staleToken));

            if(device != null) {
                device.markAsDirty();
                device.token = updatedToken;
                mEbeanServer.update(device, transaction);
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
            Query<Device> deviceQuery = mEbeanServer.createQuery(Device.class)
                    .where()
                    .eq("token", deviceToken)
                    .query();

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

        if (device.token != null || device.deviceId != null) {
            try {
                // Build a query depending on if we have a token, and or device identifier.
                ExpressionList<Device> existingDeviceExpression = mEbeanServer
                        .createQuery(Device.class)
                        .where()
                        .disjunction();

                if (device.deviceId != null && !device.deviceId.isEmpty()) {
                    existingDeviceExpression.eq("deviceId", device.deviceId);
                }

                if (device.token != null && !device.token.isEmpty()) {
                    existingDeviceExpression.eq("token", device.token);
                }

                existingDeviceExpression.endJunction();
                Device foundDevice = existingDeviceExpression.findUnique();

                // Update an existing device if it exists.
                if (foundDevice != null) {
                    device.id = foundDevice.id;
                    mEbeanServer.update(device, transaction);

                } else {
                    mEbeanServer.save(device, transaction);
                }

                transaction.commit();
                transaction.end();
                return true;

            } catch (Exception e) {
                Logger.error(String.format("Error saving device device for %s.", device.deviceId), e);
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
