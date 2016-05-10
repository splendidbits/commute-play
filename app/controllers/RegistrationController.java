package controllers;

import services.PushMessageManager;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.devices.Device;
import play.mvc.Controller;
import play.mvc.Result;
import pushservices.enums.PlatformType;
import services.AccountsDao;
import services.DeviceDao;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
@SuppressWarnings("unused")
public class RegistrationController extends Controller {

    @Inject
    private AccountsDao mAccountsDao;

    @Inject
    private DeviceDao mDeviceDao;

    @Inject
    private PushMessageManager mPushMessageManager;

    // Return results enum
    private enum RegistrationResult {
        OK(ok("Success")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request")),
        BAD_ACCOUNT(badRequest("No platform account registered for api_key")),
        OVERDRAWN_ACCOUNT(badRequest("Over quota for account. Email help@splendidbits.co")),
        BAD_REGISTRATION_REQUEST(badRequest("Error adding device registration")),
        UNKNOWN_ERROR(badRequest("Unknown error saving the device registration."));

        public Result mResultValue;

        RegistrationResult(play.mvc.Result resultValue) {
            mResultValue = resultValue;
        }
    }

    /**
     * Register a client with the commute GCM server. Saves important
     * token information along with a timestamp.
     *
     * @return A Result.
     */
    @SuppressWarnings("Convert2Lambda,unused")
    public CompletionStage<Result> register() {
        CompletionStage<RegistrationResult> promiseOfRegistration = initiateRegistration();

        return promiseOfRegistration.thenApplyAsync(new Function<RegistrationResult, Result>() {
            @Override
            public Result apply(RegistrationResult registrationResult) {
                return registrationResult.mResultValue;
            }
        });
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
