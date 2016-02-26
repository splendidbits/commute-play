package controllers;

import models.registrations.Registration;
import play.mvc.Controller;
import play.mvc.Result;
import services.DeviceSubscriptionsService;

import java.util.Map;

/**
 * The public API endpoint controller that handles devices registering
 * with the commute server.
 */
public class RegistrationController extends Controller {
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String REGISTRATION_TOKEN_KEY = "devregid";

    // Return results
    private static final Result MISSING_PARAMS_RESULT = badRequest("Invalid registration parameters in request.");
    private static final Result BAD_CLIENT_RESULT = badRequest("Calling client is invalid.");

    /**
     * Register a client with the commute GCM server. Saves important
     * token information along with a timestamp.
     *
     * @return A Result.
     */
    public Result register() {
        // Grab the header that the client has sent.
        String userAgent = request().getHeader("User-Agent");
        Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();

        if (clientRequestBody == null) {
            return MISSING_PARAMS_RESULT;
        }

        String deviceId = clientRequestBody.get(DEVICE_UUID_KEY)[0];
        String registrationId = clientRequestBody.get(REGISTRATION_TOKEN_KEY)[0];

        // Check that there was a valid registration token and device uuid.
        if ((registrationId == null || registrationId.isEmpty()) ||
                (deviceId == null || deviceId.isEmpty())) {
            return MISSING_PARAMS_RESULT;
        }

        Registration newRegistration = new Registration(deviceId, registrationId);
        DeviceSubscriptionsService subscriptionService = new DeviceSubscriptionsService();

        boolean success = subscriptionService.addRegistration(newRegistration);
        return success ? ok() : badRequest();
    }

}
