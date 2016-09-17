package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import dao.AccountsDao;
import dao.DeviceDao;
import enums.pushservices.Failure;
import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.AlertHelper;
import helpers.RequestHelper;
import helpers.pushservices.PlatformMessageBuilder;
import interfaces.pushservices.TaskQueueCallback;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.devices.Device;
import models.pushservices.*;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.PushMessageManager;
import services.pushservices.TaskQueue;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;


/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
@SuppressWarnings("unused")
public class DeviceController extends Controller {
    private AccountsDao mAccountsDao;
    private DeviceDao mDeviceDao;
    private TaskQueue mTaskQueue;
    private PushMessageManager mPushMessageManager;

    @Inject
    public DeviceController(AccountsDao accountsDao, DeviceDao deviceDao, TaskQueue taskQueue, PushMessageManager pushMessageManager) {
        mAccountsDao = accountsDao;
        mDeviceDao = deviceDao;
        mTaskQueue = taskQueue;
        mPushMessageManager = pushMessageManager;
    }

    // Return results enum
    private enum RegistrationResult {
        OK(ok("Success")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request")),
        BAD_ACCOUNT(unauthorized("No platform account registered for api_key")),
        OVERDRAWN_ACCOUNT(paymentRequired("Over quota for account. Email help@splendidbits.co")),
        BAD_REGISTRATION_REQUEST(badRequest("Error adding device registration")),
        UNKNOWN_ERROR(badRequest("Unknown error saving the device registration."));

        public Result mResultValue;

        RegistrationResult(play.mvc.Result resultValue) {
            mResultValue = resultValue;
        }
    }

    /**
     * Endpoint for cleaning devices by sending a ping GCM and un-registering / un-subscribing if needed/
     *
     * @return A result for if the subscription request succeeded or failed.
     */
    @SuppressWarnings("Convert2Lambda")
    public CompletionStage<Result> cleanDevices() {
        CompletableFuture<Result> completableResult = null;

        try {
            String rawIp = request().remoteAddress();
            InetAddress clientIp = InetAddress.getByName(rawIp);
            completableResult = CompletableFuture.supplyAsync(new Supplier<Result>() {

                @Override
                public Result get() {
                    if (!clientIp.getHostName().equals("localhost")) {
                        return badRequest("This is a private API");
                    }

                    Set<String> deviceTokens = new HashSet<>();
                    List<Device> foundDevices = mDeviceDao.getAllDevices();
                    for (Device device : foundDevices) {
                        deviceTokens.add(device.token);
                    }

                    Account accountDetails = mAccountsDao.getAccountForEmail("daniel@staticfish.com");
                    if (accountDetails != null &&
                            accountDetails.platformAccounts != null &&
                            !accountDetails.platformAccounts.isEmpty()) {

                        Credentials credentials = AlertHelper.getMessageCredentials(accountDetails.platformAccounts.get(0));
                        Message pingAllMessage = new PlatformMessageBuilder.Builder()
                                .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                                .putData("ping_message", "ping")
                                .setCollapseKey("ping")
                                .setShouldDelayWhileIdle(false)
                                .setDeviceTokens(deviceTokens)
                                .setPlatformCredentials(credentials)
                                .build();

                        Task pingAllTask = new Task("pingAll");
                        pingAllTask.messages.add(pingAllMessage);

                        try {
                            mTaskQueue.queueTask(pingAllTask, new DevicePingCallback());

                        } catch (TaskValidationException e) {
                            Logger.error("Task Validation Exception", e);
                        }
                    }
                    return ok("Success");
                }
            });

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return completableResult;
    }

    /**
     * Callbacks for the pingAll function
     */
    private class DevicePingCallback implements TaskQueueCallback {

        @Override
        public void updatedRegistrations(@Nonnull Map<Recipient, Recipient> updatedRegistrations) {
            Logger.debug("Registration update response from ping. Updating.");
            for (Map.Entry<Recipient, Recipient> updatedRegistration : updatedRegistrations.entrySet()) {
                Recipient staleRecipient = updatedRegistration.getKey();
                Recipient newRecipient = updatedRegistration.getValue();

                mDeviceDao.saveUpdatedToken(staleRecipient.token, newRecipient.token);
            }
        }

        @Override
        public void invalidRecipients(@Nonnull List<Recipient> recipients) {
            Logger.debug("Invalid recipients response from ping. Deleting.");
            for (Recipient recipient : recipients) {
                mDeviceDao.removeDevice(recipient.token);
            }
        }

        @Override
        public void failedRecipient(@Nonnull Recipient recipient, @Nonnull PlatformFailure failure) {
            if (failure.failure.equals(Failure.RECIPIENT_NOT_REGISTERED) ||
                    failure.failure.equals(Failure.RECIPIENT_REGISTRATION_INVALID)) {
                Logger.debug("Failed recipient response from ping. Deleting.");
                mDeviceDao.removeDevice(recipient.token);
            }
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {

        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {

        }
    }

    /**
     * Register a client with the commute GCM server. Saves important
     * token information along with a timestamp.
     *
     * @return A Result.
     */
    public CompletionStage<Result> register() {
        CompletionStage<RegistrationResult> promiseOfRegistration = initiateRegistration();
        return promiseOfRegistration.thenApplyAsync(registrationResult -> registrationResult.mResultValue);
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
        return mAccountsDao.getAccountForKey(foundApiKey);
    }

    public CompletionStage<Result> getDevices() {
        Account requestAccount = getRequestApiAccount();
        if (requestAccount == null) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        List<Device> devicesList = mDeviceDao.getAccountDevices(requestAccount.apiKey, 0, null);
        JsonNode deviceJsonArray = RequestHelper.removeSubscriptionRouteAlerts(Json.toJson(devicesList));

        return CompletableFuture.completedFuture(ok(deviceJsonArray));
    }

    public CompletionStage<Result> getDevicesPage(int page) {
        Account requestAccount = getRequestApiAccount();
        if (requestAccount == null) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        List<Device> devicesList = mDeviceDao.getAccountDevices(requestAccount.apiKey, page, null);
        JsonNode deviceJsonArray = RequestHelper.removeSubscriptionRouteAlerts(Json.toJson(devicesList));

        return CompletableFuture.completedFuture(ok(deviceJsonArray));
    }

    public CompletionStage<Result> getDevicesPageAgency(int page, Integer agencyId) {
        Account requestAccount = getRequestApiAccount();
        if (requestAccount == null) {
            return CompletableFuture.completedFuture(unauthorized());
        }

        List<Device> devicesList = mDeviceDao.getAccountDevices(requestAccount.apiKey, page, agencyId);
        JsonNode deviceJsonArray = RequestHelper.removeSubscriptionRouteAlerts(Json.toJson(devicesList));

        return CompletableFuture.completedFuture(ok(deviceJsonArray));
    }

    /**
     * Perform registration action for new device.
     *
     * @return CompletionStage<RegistrationResult> result of registration action.
     */
    @Nonnull
    private CompletionStage<RegistrationResult> initiateRegistration() {
        final String API_KEY = "api_key";
        final String DEVICE_UUID_KEY = "device_uuid";
        final String REGISTRATION_TOKEN_KEY = "registration_id";
        final String APP_ID_KEY = "app_id";
        final String APP_USER_ID_KEY = "user_id";

        Map<String, String[]> requestMap = request().body().asFormUrlEncoded();
        if (requestMap == null) {
            return CompletableFuture.completedFuture(RegistrationResult.MISSING_PARAMS_RESULT);
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
            return CompletableFuture.completedFuture(RegistrationResult.MISSING_PARAMS_RESULT);
        }

        Account account = mAccountsDao.getAccountForKey(apiKey);
        if (account == null || !account.active) {
            return CompletableFuture.completedFuture(RegistrationResult.BAD_ACCOUNT);
        }

        Device device = new Device(deviceId, registrationId);
        device.account = account;
        device.appKey = appKey;
        device.userKey = userKey;

        // Return error if there were not platform accounts for Account.
        if (account.platformAccounts == null || account.platformAccounts.isEmpty()) {
            return CompletableFuture.completedFuture(RegistrationResult.BAD_ACCOUNT);
        }

        // Return error on error saving registration.
        boolean persistSuccess = mDeviceDao.saveDevice(device);
        if (!persistSuccess) {
            return CompletableFuture.completedFuture(RegistrationResult.UNKNOWN_ERROR);
        }

        // Send update using one of each platform.
        for (PlatformAccount platformAccount : account.platformAccounts) {
            if (platformAccount.platformType.equals(PlatformType.SERVICE_GCM)) {
                mPushMessageManager.sendRegistrationConfirmMessage(device, platformAccount);
                break;
            }
        }

        return CompletableFuture.completedFuture(RegistrationResult.OK);
    }
}
