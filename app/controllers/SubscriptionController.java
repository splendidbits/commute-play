package controllers;

import models.alerts.Route;
import models.registrations.Registration;
import models.registrations.Subscription;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AgencyDatabaseService;
import services.AccountService;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * The public API endpoint controller that handles devices subscribing to agency routes
 * with the commute server.
 */
public class SubscriptionController extends Controller {
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String ROUTE_LIST_KEY = "routeslist";
    private static final String AGENCY_NAME_KEY = "agencyname";

    // Response enum.
    private enum SubscriptionResponse {
        RESULT_OK("Success"),
        MISSING_PARAMS_RESULT("Invalid registration parameters in request."),
        NO_REGISTRATION_RESULT("No registered device found for device ID.");

        private String mValue = null;

        SubscriptionResponse(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }
    }

    /**
     * Endpoint for subscribing a deviceId to a route.
     *
     * @return A result for if the subscription request succeeded or failed.
     */
    public Result subscribe() {
        Http.RequestBody requestBody = request().body();
        SubscriptionResponse dataValidity = validateInputData(requestBody);

        if (dataValidity.equals(SubscriptionResponse.RESULT_OK)) {
            Map<String, String[]> formEncodedMap = requestBody.asFormUrlEncoded();

            String deviceId = formEncodedMap.get(DEVICE_UUID_KEY)[0].trim().toLowerCase();
            String[] routes = formEncodedMap.get(ROUTE_LIST_KEY) != null
                    ? formEncodedMap.get(ROUTE_LIST_KEY)[0].trim().split(" ")
                    : null;

            // Check the agency name. If it's null, it's an older SEPTA Instant client, so add 'septa'.
            String[] agencyValue = formEncodedMap.get(AGENCY_NAME_KEY);
            String agencyName = agencyValue != null ? agencyValue[0].trim().toLowerCase() : "septa";

            // Check that the device is already registered.
            AccountService subscriptionService = new AccountService();
            Registration existingRegistration = subscriptionService.getRegistration(deviceId);
            if (existingRegistration == null) {
                return badRequest(SubscriptionResponse.NO_REGISTRATION_RESULT.getValue());
            }

            AgencyDatabaseService agencyService = AgencyDatabaseService.getInstance();

            // Get a list of all the valid routes from the sent primitive array. Add them to the subscription.
            List<Route> validRoutes = agencyService.getRouteAlerts(agencyName, routes);
            if (validRoutes != null) {
                models.registrations.Subscription subscription = new Subscription();
                subscription.registration = existingRegistration;
                subscription.routes = validRoutes;
                subscription.timeSubscribed = Calendar.getInstance();

                // Persist the subscription.
                subscriptionService.addSubscription(subscription);
            }
        }
        return ok(dataValidity.getValue());
    }

    /**
     * Sanity check the data we have received from the client.
     *
     * @param requestBody the body of the incoming request.
     * @return enum success result type.
     */
    private SubscriptionResponse validateInputData(Http.RequestBody requestBody) {
        if (requestBody != null) {
            Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();

            if (clientRequestBody == null) {
                return SubscriptionResponse.MISSING_PARAMS_RESULT;
            }

            // TODO: Re-add check for agency name when it has been added to the client.
            // Check that there was a valid list of routes and device uuid.
            if ((!clientRequestBody.containsKey(DEVICE_UUID_KEY)) ||
                    //(!clientRequestBody.containsKey(AGENCY_NAME_KEY)) ||
                    (!clientRequestBody.containsKey(ROUTE_LIST_KEY))) {
                return SubscriptionResponse.MISSING_PARAMS_RESULT;
            }
        }
        return SubscriptionResponse.RESULT_OK;
    }
}
