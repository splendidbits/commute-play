package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import models.devices.Device;
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A DAO class for both device device / subscription data.
 */
public class DeviceDao {
    private static final String TAG = DeviceDao.class.getSimpleName();

    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public DeviceDao(EbeanServer ebeanServer, Log log) {
        mEbeanServer = ebeanServer;
        mLog = log;
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
            Device device = mEbeanServer.find(Device.class)
                    .fetch("subscriptions")
                    .where()
                    .eq("deviceId", deviceId)
                    .findUnique();

            String logString = device != null
                    ? String.format("Found device with deviceId %s", device.deviceId)
                    : String.format("No device found deviceId %s", deviceId);

            mLog.d(TAG, logString);
            return device;

        } catch (Exception e) {
            mLog.e(TAG, "Error persisting subscription", e);
        }
        return null;
    }


    /**
     * Delete a device device, and all route subscriptions.
     *
     * @param deviceToken the device token for the device.
     * @return true or false depending on if the device was deleted.
     */
    public boolean removeDevice(@Nonnull String deviceToken) {
        try {
            Device foundDevice = mEbeanServer.createQuery(Device.class)
                    .where()
                    .eq("token", deviceToken)
                    .findUnique();

            if (foundDevice != null) {
                mEbeanServer.delete(foundDevice);
                mLog.d(TAG, String.format("Removed device for deviceId %s,", foundDevice.deviceId));
                return true;
            }

        } catch (Exception e) {
            mLog.e(TAG, String.format("Error deleting device for %s.", deviceToken), e);
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
                    mEbeanServer.update(device);

                } else {
                    mEbeanServer.insert(device);
                }
                return true;

            } catch (Exception e) {
                mLog.e(TAG, String.format("Error saving device device for %s.", device.deviceId), e);
            }
        }
        return false;
    }

}
