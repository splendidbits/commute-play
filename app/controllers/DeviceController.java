package controllers;

import agency.inapp.InAppMessageUpdate;
import dao.AccountDao;
import dao.DeviceDao;
import enums.AlertType;
import enums.pushservices.Failure;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.AlertHelper;
import interfaces.pushservices.TaskQueueCallback;
import models.AlertModifications;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import models.pushservices.db.Task;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.PushMessageManager;
import services.fluffylog.Logger;
import services.pushservices.TaskQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


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
        CompletionStage<DeviceControllerResult> promiseOfRegistration = initiateRegistration();
        return promiseOfRegistration.thenApplyAsync(deviceControllerResult -> deviceControllerResult.value);
    }

    /**
     * Requests that all known devices re-send their route subscriptions by pinging each one with a fake
     * route. TODO: create a real non-fallback key to perform this action.
     *
     * @return A Result.
     */
    public CompletionStage<Result> requestDeviceSubscriptionResend() {
        final Http.Context inboundReqContext = ctx();

        return CompletableFuture.supplyAsync(() -> inboundReqContext)
                .thenApply(context -> {
                    String resendSubscriptionsName = "route_resend_subscriptions";
                    Http.RequestBody requestBody = context.request().body();

                    String apiKey = (requestBody != null && requestBody.asMultipartFormData() != null &&
                            requestBody.asMultipartFormData().asFormUrlEncoded().containsKey(API_KEY))
                            ? requestBody.asMultipartFormData().asFormUrlEncoded().get(API_KEY)[0]
                            : null;

                    if (apiKey == null) {
                        return DeviceControllerResult.MISSING_PARAMS_RESULT.value;
                    }

                    // Return error if there is no Account or platform accounts for apiKey.
                    Account account = mAccountDao.getAccountForKey(apiKey);
                    if (account == null || !account.active || account.platformAccounts == null || account.platformAccounts.isEmpty()) {
                        return DeviceControllerResult.BAD_ACCOUNT.value;
                    }

                    // Fetch all devices for the given account.
                    List<Device> allDevices = mDeviceDao.getAccountDevices(account.apiKey, 2);
                    if (allDevices.isEmpty()) {
                        return DeviceControllerResult.OK.value;
                    }

                    // Create an agency alert modifications for a route that doesn't exist.
                    Route route = new Route(resendSubscriptionsName, resendSubscriptionsName);
                    route.routeName = resendSubscriptionsName;
                    route.agency = new Agency(InAppMessageUpdate.AGENCY_ID);

                    // This will invoke each device to resubscribe.
                    Alert updateAlert = new Alert();
                    updateAlert.highPriority = false;
                    updateAlert.messageTitle = resendSubscriptionsName;
                    updateAlert.type = AlertType.TYPE_IN_APP;
                    updateAlert.messageBody = resendSubscriptionsName;

                    route.alerts = Collections.singletonList(updateAlert);

                    AlertModifications alertUpdate = new AlertModifications(InAppMessageUpdate.AGENCY_ID);
                    alertUpdate.addUpdatedRoute(route);

                    // Send update using one of each platform.
                    for (PlatformAccount platformAccount : account.platformAccounts) {
                        if (platformAccount.platformType.equals(PlatformType.SERVICE_GCM)) {

                            // Create push services messages for the update.
                            List<Message> messages = AlertHelper.getAlertMessages(route, allDevices, platformAccount, true);
                            if (messages != null && !messages.isEmpty()) {
                                Task task = new Task(resendSubscriptionsName);
                                task.messages = messages;
                                task.priority = Task.TASK_PRIORITY_LOW;

                                try {
                                    mTaskQueue.queueTask(task, new PingAllDevicesCallback());

                                } catch (TaskValidationException e) {
                                    Logger.error(String.format("Error sending Task to pushservices: %s", e.getMessage()));
                                }
                            }
                        }
                    }

                    return DeviceControllerResult.OK.value;
                });
    }

    /**
     * Get the API account for the web request.
     *
     * @return Account for apiKey if found.
     */
    @Nullable
    private Account getRequestApiAccount() {
        final Set<Map.Entry<String, String[]>> entries = request().queryString().entrySet();
        String foundApiKey = null;

        for (Map.Entry<String, String[]> entry : entries) {
            final String key = entry.getKey();
            final String value = Arrays.deepToString(entry.getValue());

            if (key.toLowerCase().equals("apikey") && !value.isEmpty()) {
                foundApiKey = value.substring(1, value.length() - 1);
            }
        }
        return mAccountDao.getAccountForKey(foundApiKey);
    }

    /**
     * Perform registration action for new device.
     *
     * @return CompletionStage<RegistrationResult> result of registration action.
     */
    @Nonnull
    private CompletionStage<DeviceControllerResult> initiateRegistration() {

        Map<String, String[]> requestMap = request().body().asFormUrlEncoded();
        if (requestMap == null) {
            return CompletableFuture.completedFuture(DeviceControllerResult.MISSING_PARAMS_RESULT);
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
            return CompletableFuture.completedFuture(DeviceControllerResult.MISSING_PARAMS_RESULT);
        }

        Account account = mAccountDao.getAccountForKey(apiKey);
        if (account == null || !account.active) {
            return CompletableFuture.completedFuture(DeviceControllerResult.BAD_ACCOUNT);
        }

        Device device = new Device(deviceId, registrationId);
        device.account = account;
        device.appKey = appKey;
        device.userKey = userKey;

        // Return error if there were no platform accounts for apiKey.
        if (account.platformAccounts == null || account.platformAccounts.isEmpty()) {
            return CompletableFuture.completedFuture(DeviceControllerResult.BAD_ACCOUNT);
        }

        // Return error on error saving registration.
        boolean persistSuccess = mDeviceDao.saveDevice(device);
        if (!persistSuccess) {
            return CompletableFuture.completedFuture(DeviceControllerResult.UNKNOWN_ERROR);
        }

        // Send update using one of each platform.
        for (PlatformAccount platformAccount : account.platformAccounts) {
            if (platformAccount.platformType.equals(PlatformType.SERVICE_GCM)) {
                mPushMessageManager.sendRegistrationConfirmMessage(device, platformAccount);
                break;
            }
        }

        return CompletableFuture.completedFuture(DeviceControllerResult.OK);
    }


    /**
     * Callback received from provider with results of device message.
     */
    private class PingAllDevicesCallback implements TaskQueueCallback {

        @Override
        public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRecipients) {
            Logger.info(String.format("%d recipients require registration updates.", updatedRecipients.size()));

            for (UpdatedRecipient recipientUpdate : updatedRecipients) {
                Recipient staleRecipient = recipientUpdate.getStaleRecipient();
                Recipient updatedRecipient = recipientUpdate.getUpdatedRecipient();

                mDeviceDao.saveUpdatedToken(staleRecipient.token, updatedRecipient.token);
            }
        }

        @Override
        public void failedRecipients(@Nonnull List<FailedRecipient> failedRecipients) {
            Logger.warn(String.format("%d recipients failed fatally.", failedRecipients.size()));

            for (FailedRecipient failedRecipient : failedRecipients) {
                Recipient recipient = failedRecipient.getRecipient();
                Failure failure = failedRecipient.getFailure().failure;

                if (failure != null && (failure == Failure.RECIPIENT_REGISTRATION_INVALID ||
                        failure == Failure.RECIPIENT_NOT_REGISTERED ||
                        failure == Failure.MESSAGE_PACKAGE_INVALID)) {
                    mDeviceDao.removeDevice(recipient.token);
                }
            }
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {
            Logger.debug(String.format("Message %d completed.", originalMessage.id));
        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
            Logger.debug(String.format("Message %d failed - %s.", originalMessage.id, failure.failureMessage));
        }
    }
}
