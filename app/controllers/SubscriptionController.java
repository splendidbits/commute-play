package controllers;

import models.alerts.Route;
import models.registrations.Registration;
import models.registrations.Subscription;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AgencyService;
import services.AccountService;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The public API endpoint controller that handles devices subscribing to agency routes
 * with the commute server.
 */
public class SubscriptionController extends Controller {
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String ROUTE_LIST_KEY = "routeslist";
    private static final String AGENCY_NAME_KEY = "agencyname";

    // Return results enum
    private enum SubscriptionResult {
        OK(ok("Success")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request.")),
        NO_REGISTRATION_RESULT(badRequest("No registered device found for device ID."));

        public Result mResultValue;
        SubscriptionResult(play.mvc.Result resultValue) {
            mResultValue = resultValue;
        }
    }

    @Inject
    private AgencyService mAgencyService;

    @Inject
    private AccountService mAccountService;

    /**
     * Endpoint for subscribing a deviceId to a route.
     *
     * @return A result for if the subscription request succeeded or failed.
     */
    public CompletionStage<Result> subscribe() {
        CompletionStage<SubscriptionResult> promiseOfSubscription = initiateSubscription();

        return promiseOfSubscription.thenApplyAsync(new Function<SubscriptionResult, Result>() {
            @Override
            public Result apply(SubscriptionResult result) {
                return result.mResultValue;
            }
        });
    }

    /**
     * Perform subscription action for a registered device.
     *
     * @return CompletionStage<SubscriptionResult> result of registration action.
     */
    private CompletionStage<SubscriptionResult> initiateSubscription() {
        Http.RequestBody requestBody = request().body();
        SubscriptionResult subscriptionResult = validateInputData(requestBody);

        if (subscriptionResult.equals(SubscriptionResult.OK)) {
            Map<String, String[]> formEncodedMap = requestBody.asFormUrlEncoded();

            String deviceId = formEncodedMap.get(DEVICE_UUID_KEY)[0].trim().toLowerCase();
            String[] routes = formEncodedMap.get(ROUTE_LIST_KEY) != null
                    ? formEncodedMap.get(ROUTE_LIST_KEY)[0].trim().split(" ")
                    : null;

            // Check the agency name. If it's null, it's an older SEPTA Instant client, so add 'septa'.
            String[] agencyValue = formEncodedMap.get(AGENCY_NAME_KEY);
            String agencyName = agencyValue != null ? agencyValue[0].trim().toLowerCase() : "septa";

            // Check that the device is already registered.
            Registration existingRegistration = mAccountService.getRegistration(deviceId);
            if (existingRegistration == null) {
                return CompletableFuture.completedFuture(SubscriptionResult.NO_REGISTRATION_RESULT);
            }

            // Get a list of all the valid routes from the sent primitive array. Add them to the subscription.
            List<Route> validRoutes = mAgencyService.getRouteAlerts(agencyName, routes);
            if (validRoutes != null) {
                models.registrations.Subscription subscription = new Subscription();
                subscription.registration = existingRegistration;
                subscription.routes = validRoutes;
                subscription.timeSubscribed = Calendar.getInstance();

                // Persist the subscription.
                mAccountService.addSubscription(subscription);
            }
        }
        return CompletableFuture.completedFuture(subscriptionResult);
    }

    /**
     * Sanity check the data we have received from the client.
     *
     * @param requestBody the body of the incoming request.
     * @return enum success result type.
     */
    private SubscriptionResult validateInputData(Http.RequestBody requestBody) {
        if (requestBody != null) {
            Map<String, String[]> clientRequestBody = request().body().asFormUrlEncoded();

            if (clientRequestBody == null) {
                return SubscriptionResult.MISSING_PARAMS_RESULT;
            }

            // TODO: Re-add check for agency name when it has been added to the client.
            // Check that there was a valid list of routes and device uuid.
            if ((!clientRequestBody.containsKey(DEVICE_UUID_KEY)) ||
                    //(!clientRequestBody.containsKey(AGENCY_NAME_KEY)) ||
                    (!clientRequestBody.containsKey(ROUTE_LIST_KEY))) {
                return SubscriptionResult.MISSING_PARAMS_RESULT;
            }
        }
        return SubscriptionResult.OK;
    }
}
