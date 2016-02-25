package controllers;

import models.alerts.Alert;
import models.registrations.Registration;
import play.mvc.Controller;
import play.mvc.Result;
import services.SubscriptionsDatabaseService;

import java.util.*;

/**
 * The public API endpoint controller that handles devices subscribing to agency routes
 * with the commute server.
 */
public class SubscriptionController extends Controller {
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String ROUTE_LIST_KEY = "routeslist";

    // Return results
    private static final Result MISSING_PARAMS_RESULT = badRequest("Invalid registration parameters in request.");
    private static final Result NO_REGISTRATION_RESULT = badRequest("No registered device found for device.");

    public Result subscribe() {
        // Grab the header that the client has sent.
        String userAgent = request().getHeader("User-Agent");
        Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();

        if (clientRequestBody == null) {
            return MISSING_PARAMS_RESULT;
        }

        String deviceId = clientRequestBody.get(DEVICE_UUID_KEY)[0];
        String[] routes = clientRequestBody.get(ROUTE_LIST_KEY);

        // Check that there was a valid registration token and device uuid.
        if ((routes == null) ||
                (deviceId == null || deviceId.isEmpty())) {
            return MISSING_PARAMS_RESULT;
        }

        SubscriptionsDatabaseService subscriptionService = new SubscriptionsDatabaseService();
        Registration existingRegistration = subscriptionService.getRegistration(deviceId);
        if (existingRegistration == null) {
            return NO_REGISTRATION_RESULT;
        }

        List<String> receivedRoutes = Collections.singletonList(deviceId);
        List<Alert> alertsToSubscribe = new ArrayList<>();

        for (String routeId : receivedRoutes) {
            routeId = routeId.trim().toLowerCase();

            if (!routeId.isEmpty()) {


            }
        }


        return ok();
    }

}
