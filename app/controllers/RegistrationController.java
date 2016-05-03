package controllers;

import models.accounts.Account;
import models.registrations.Registration;
import play.mvc.Controller;
import play.mvc.Result;
import services.AccountService;
import dispatcher.processors.PushMessageManager;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
public class RegistrationController extends Controller {
    private static final String API_KEY = "api_key";
    private static final String DEVICE_UUID_KEY = "device_uuid";
    private static final String REGISTRATION_TOKEN_KEY = "registration_id";

    @Inject
    private AccountService mAccountService;

    @Inject
    private PushMessageManager mPushMessageManager;

    // Return results enum
    private enum RegistrationResult {
        OK(ok("Success")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request")),
        BAD_CLIENT_RESULT(badRequest("Calling client is invalid")),
        BAD_ACCOUNT(badRequest("No account for api_key")),
        OVERDRAWN_ACCOUNT(badRequest("Over quota for account. Email help@splendidbits.co"));

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
    @SuppressWarnings("Convert2Lambda")
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
    private CompletionStage<RegistrationResult> initiateRegistration() {
        Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();
        if (clientRequestBody == null) {
            return CompletableFuture.completedFuture(RegistrationResult.MISSING_PARAMS_RESULT);
        }

        String deviceId = clientRequestBody.get(DEVICE_UUID_KEY)[0];
        String registrationId = clientRequestBody.get(REGISTRATION_TOKEN_KEY)[0];

        // Check that there was a valid registration token and device uuid.
        if ((registrationId == null || registrationId.isEmpty()) ||
                (deviceId == null || deviceId.isEmpty())) {
            return CompletableFuture.completedFuture(RegistrationResult.MISSING_PARAMS_RESULT);
        }

        /*
         * For now, get the default commute account for all requests.
         * TODO: Remove this when all clients have been upgraded to send API key.
         */
        String apiKey = clientRequestBody.get(API_KEY) == null ? null : clientRequestBody.get(API_KEY)[0];
        Account account = apiKey != null
                ? mAccountService.getAccountByApi(apiKey)
                : mAccountService.getAccountByEmail("daniel@staticfish.com");

        if (account == null || !account.active) {
            return CompletableFuture.completedFuture(RegistrationResult.BAD_ACCOUNT);
        }

        Registration newRegistration = new Registration(deviceId, registrationId);
        newRegistration.account = account;
        boolean persistSuccess = mAccountService.addRegistration(newRegistration);

        if (persistSuccess && account.platformAccounts != null) {
            mPushMessageManager.sendRegistrationConfirmation(newRegistration, account.platformAccounts);
            return CompletableFuture.completedFuture(RegistrationResult.OK);

        } else {
            return CompletableFuture.completedFuture(RegistrationResult.BAD_CLIENT_RESULT);
        }
    }
}
