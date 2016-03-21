package controllers;

import models.accounts.Account;
import models.registrations.Registration;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import services.AccountServiceDao;

import java.util.Map;

/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
public class RegistrationController extends Controller {
    private static final String API_KEY = "api_key";
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String REGISTRATION_TOKEN_KEY = "devregid";

    // Return results
    private static final Result MISSING_PARAMS_RESULT = badRequest("Invalid registration parameters in request.");
    private static final Result BAD_CLIENT_RESULT = badRequest("Calling client is invalid.");
    private static final Result BAD_ACCOUNT = badRequest("No account for api_key");
    private static final Result OVERDRAWN_ACCOUNT = badRequest("Over quota for account. Email help@splendidbits.co");

    /**
     * Register a client with the commute GCM server. Saves important
     * token information along with a timestamp.
     *
     * @return A Result.
     */
    @Transactional
    public Result register() {
        AccountServiceDao accountServiceDao = new AccountServiceDao();

        Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();
        if (clientRequestBody == null) {
            return MISSING_PARAMS_RESULT;
        }

        String deviceId = clientRequestBody.get(DEVICE_UUID_KEY)[0];
        String registrationId = clientRequestBody.get(REGISTRATION_TOKEN_KEY)[0];
        String apiKey = clientRequestBody.get(API_KEY) == null ? null : clientRequestBody.get(API_KEY)[0];

        // Check that there was a valid registration token and device uuid.
        if ((registrationId == null || registrationId.isEmpty()) ||
                (deviceId == null || deviceId.isEmpty())) {
            return MISSING_PARAMS_RESULT;
        }

        /*
         * Hack until all the clients have added an API key.
         * TODO: Remove this when all clients have been upgraded to send API key.
         */
        Account account = apiKey != null
                ? accountServiceDao.getAccount(apiKey, null)
                : accountServiceDao.getAccount(null, "daniel@staticfish.com");

        if (account == null) {
            return BAD_ACCOUNT;
        }

        Registration newRegistration = new Registration(deviceId, registrationId);
        newRegistration.account = account;
        boolean persistSuccess = accountServiceDao.addRegistration(newRegistration);

        return persistSuccess ? ok() : badRequest();
    }

}
