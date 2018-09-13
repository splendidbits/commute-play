package controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import dao.AccountDao;
import dao.DeviceDao;
import enums.pushservices.FailureType;
import enums.pushservices.PlatformType;
import exceptions.pushservices.MessageValidationException;
import helpers.AlertHelper;
import interfaces.pushservices.TaskQueueListener;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.devices.Device;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.PushMessageManager;
import services.pushservices.TaskQueue;


/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
public class DeviceController extends Controller {
    private static final String API_KEY = "api_key";
    private static final String DEVICE_UUID_KEY = "device_uuid";
    private static final String REGISTRATION_TOKEN_KEY = "registration_id";
    private static final String APP_ID_KEY = "app_id";
    private static final String APP_USER_ID_KEY = "user_id";

    private AccountDao mAccountDao;
    private DeviceDao mDeviceDao;
    private PushMessageManager mPushMessageManager;
    private TaskQueue mTaskQueue;

    @Inject
    public DeviceController(AccountDao accountDao, DeviceDao deviceDao, PushMessageManager pushMessageManager, TaskQueue taskQueue) {
        mAccountDao = accountDao;
        mDeviceDao = deviceDao;
        mPushMessageManager = pushMessageManager;
        mTaskQueue = taskQueue;
    }

    // Return results enum
    private enum DeviceControllerResult {
        OK(ok("Success")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request")),
        BAD_ACCOUNT(unauthorized("No platform account registered for api_key")),
        OVERDRAWN_ACCOUNT(paymentRequired("Over quota for account. Email help@splendidbits.co")),
        BAD_REGISTRATION_REQUEST(badRequest("Error adding device registration")),
        UNKNOWN_ERROR(badRequest("Unknown error saving the device registration."));

        public Result value;

        DeviceControllerResult(play.mvc.Result resultValue) {
            value = resultValue;
        }
    }

    /**
     * Register a client with the commute GCM server. Saves important
     * token information along with a timestamp.
     *
     * @return A Result.
     */
    public CompletionStage<Result> register() {
        CompletionStage<DeviceControllerResult> promiseOfRegistration = initiateRegistration(request());
        return promiseOfRegistration.thenApplyAsync(deviceControllerResult -> deviceControllerResult.value);
    }

    /**
     * Requests that all known devices re-send their route subscriptions by pinging each one with a fake
     * route. TODO: create a real non-fallback key to perform this action.
     *
     * @return A Result.
     */
    public CompletionStage<Result> requestDeviceSubscriptionResend(final @Nullable String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            if (apiKey == null || apiKey.isEmpty()) {
                return DeviceControllerResult.MISSING_PARAMS_RESULT.value;
            }

            // Return error if there is no Account or platform accounts for apiKey.
            Account account = mAccountDao.getAccountForKey(apiKey);
            if (account == null || !account.active || account.platformAccounts == null || account.platformAccounts.isEmpty()) {
                return DeviceControllerResult.BAD_ACCOUNT.value;
            }

            // Fetch all devices for the given account.
            List<Device> allDevices = mDeviceDao.getAccountDevices(account.apiKey, 1);
            if (allDevices.isEmpty()) {
                return DeviceControllerResult.OK.value;
            }

            // Send update using one of each platform.
            for (PlatformAccount platformAccount : account.platformAccounts) {
                if (platformAccount.platformType.equals(PlatformType.SERVICE_GCM)) {

                    // Create pu1sh services messages for the update.
                    List<Message> messages = AlertHelper.getResubscribeMessages(allDevices, platformAccount);
                    if (!messages.isEmpty()) {
                        try {
                            mTaskQueue.queueMessages(messages, new PingAllDevicesCallback());

                        } catch (MessageValidationException e) {
                            Logger.error(String.format("Error sending Task to push-services: %s", e.getMessage()));
                        }
                    }
                }
            }

            return DeviceControllerResult.OK.value;
        });
    }

    /**
     * Perform registration action for new device.
     *
     * @return CompletionStage<RegistrationResult> result of registration action.
     */
    @Nonnull
    private CompletionStage<DeviceControllerResult> initiateRegistration(Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String[]> requestMap = request.body().asFormUrlEncoded();
            boolean deviceUpdated = false;

            if (requestMap == null) {
                return DeviceControllerResult.MISSING_PARAMS_RESULT;
            }

            String deviceId = requestMap.get(DEVICE_UUID_KEY) != null
                    ? requestMap.get(DEVICE_UUID_KEY)[0]
                    : null;

            String registrationId = requestMap.get(REGISTRATION_TOKEN_KEY) != null
                    ? requestMap.get(REGISTRATION_TOKEN_KEY)[0]
                    : null;

            String apiKey = requestMap.get(API_KEY) != null
                    ? requestMap.get(API_KEY)[0]
                    : null;

            String appKey = requestMap.get(APP_ID_KEY) != null
                    ? requestMap.get(APP_ID_KEY)[0]
                    : null;

            String userKey = requestMap.get(APP_USER_ID_KEY) != null
                    ? requestMap.get(APP_USER_ID_KEY)[0]
                    : null;

            // Check that there was a valid registration token and device uuid.
            if (registrationId == null || deviceId == null || apiKey == null) {
                return DeviceControllerResult.MISSING_PARAMS_RESULT;
            }

            Account account = mAccountDao.getAccountForKey(apiKey);
            if (account == null || !account.active) {
                return DeviceControllerResult.BAD_ACCOUNT;
            }

            if (account.platformAccounts == null || account.platformAccounts.isEmpty()) {
                return DeviceControllerResult.BAD_ACCOUNT;
            }

            Device device = mDeviceDao.getDevice(deviceId, registrationId);
            if (device == null) {
                deviceUpdated = true;

                device = new Device();
                device.account = account;
                device.appKey = appKey;
                device.userKey = userKey;
                device.token = registrationId;
                device.deviceId = deviceId;

            } else {
                if (!device.deviceId.equals(deviceId)) {
                    device.deviceId = deviceId;
                    deviceUpdated = true;
                }
                if (!device.token.equals(registrationId)) {
                    device.token = registrationId;
                    deviceUpdated = true;
                }
            }

            // Send update using one of each platform.
            if (deviceUpdated) {
                if (!mDeviceDao.saveDevice(device)) {
                    return DeviceControllerResult.UNKNOWN_ERROR;
                }

                for (PlatformAccount platformAccount : account.platformAccounts) {
                    if (platformAccount.platformType.equals(PlatformType.SERVICE_GCM)) {
                        mPushMessageManager.sendRegistrationConfirmMessage(device, platformAccount);
                        break;
                    }
                }
            }

            return DeviceControllerResult.OK;
        });
    }

    /*
     * Callback received from provider with results of device message.
     */
    private class PingAllDevicesCallback implements TaskQueueListener {
        @Override
        public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRecipients) {
            Logger.info(String.format("%d recipients require registration updates.", updatedRecipients.size()));

            for (UpdatedRecipient recipientUpdate : updatedRecipients) {
                Recipient staleRecipient = recipientUpdate.getStaleRecipient();
                Recipient updatedRecipient = recipientUpdate.getUpdatedRecipient();

                mDeviceDao.saveUpdatedToken(staleRecipient.getToken(), updatedRecipient.getToken());
            }
        }

        @Override
        public void failedRecipients(@Nonnull List<Recipient> failedRecipients) {
            Logger.warn(String.format("%d recipients failed fatally.", failedRecipients.size()));

            for (Recipient recipient : failedRecipients) {
                FailureType failure = recipient.getPlatformFailure().getFailureType();

                if (failure != null && (failure == FailureType.RECIPIENT_REGISTRATION_INVALID ||
                        failure == FailureType.RECIPIENT_NOT_REGISTERED ||
                        failure == FailureType.MESSAGE_PACKAGE_INVALID)) {
                    mDeviceDao.removeDevice(recipient.getToken());
                }
            }
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {
            Logger.info(String.format("Message %d completed.", originalMessage.getId()));
        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
            Logger.info(String.format("Message %d failed - %s.", originalMessage.getId(), failure.getFailureMessage()));
        }
    }
}
