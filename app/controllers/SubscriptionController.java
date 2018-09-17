package controllers;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import dao.AgencyDao;
import dao.DeviceDao;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

/**
 * The public API endpoint controller that handles devices subscribing to agency routes
 * with the commute server.
 */
@SuppressWarnings("unused")
public class SubscriptionController extends Controller {
    private static final String API_KEY = "api_key";
    private static final String DEVICE_UUID_KEY = "device_uuid";
    private static final String ROUTE_LIST_KEY = "route_list";
    private static final String AGENCY_NAME_KEY = "agency_id";

    private AgencyDao mAgencyService;
    private DeviceDao mDeviceDao;

    @Inject
    public SubscriptionController(AgencyDao mAgencyService, DeviceDao mDeviceDao) {
        this.mAgencyService = mAgencyService;
        this.mDeviceDao = mDeviceDao;
    }

    // Return results enum
    enum SubscriptionResult {
        OK(ok("Success")),
        BAD_SUBSCRIPTION_REQUEST(badRequest("Error adding device subscriptions")),
        MISSING_PARAMS_RESULT(badRequest("Invalid registration parameters in request.")),
        BAD_ACCOUNT(badRequest("No account for api_key")),
        NO_REGISTRATION_RESULT(badRequest("No registered device found for device ID."));

        public Result result;

        SubscriptionResult(play.mvc.Result resultValue) {
            result = resultValue;
        }
    }

    /**
     * Endpoint for subscribing a deviceId to a route.
     *
     * @return A result for if the subscription request succeeded or failed.
     */
    public CompletionStage<Result> subscribe() {
        return initiateSubscription(request()).thenApplyAsync((stage) -> stage.result);
    }

    /**
     * Perform subscription action for a registered device.
     *
     * @return CompletionStage<SubscriptionResult> result of registration action.
     */
    private CompletionStage<SubscriptionResult> initiateSubscription(Http.Request request) {
        return CompletableFuture.supplyAsync(() -> {

            if (validateInputData(request.body())) {
                Map<String, String[]> formEncodedMap = request.body().asFormUrlEncoded();

                String agencyId = formEncodedMap.get(AGENCY_NAME_KEY) != null
                        ? formEncodedMap.get(AGENCY_NAME_KEY)[0]
                        : null;

                String deviceId = formEncodedMap.get(DEVICE_UUID_KEY) != null
                        ? formEncodedMap.get(DEVICE_UUID_KEY)[0]
                        : null;

                String[] routes = formEncodedMap.get(ROUTE_LIST_KEY) != null
                        ? formEncodedMap.get(ROUTE_LIST_KEY)[0].trim().split(" ")
                        : null;

                if (!StringUtils.isEmpty(agencyId) && !StringUtils.isEmpty(deviceId) && routes != null) {

                    // Fixes incorrect agencyId set in Android app.
                    if (agencyId.equals("1")) {
                        agencyId = "SEPTA";
                    }

                    // Check that the device is already registered.
                    Device device = mDeviceDao.getDevice(deviceId);
                    if (device == null) {
                        return SubscriptionResult.NO_REGISTRATION_RESULT;
                    }

                    // Get a list of all the valid routes from the sent primitive array. Add them to the subscription.
                    List<Route> validRoutes = mAgencyService.getRoutes(agencyId, Arrays.asList(routes));
                    List<Subscription> subscriptions = new ArrayList<>();

                    if (!validRoutes.isEmpty()) {
                        for (Route route : validRoutes) {
                            Subscription subscription = new Subscription();
                            subscription.setDevice(device);
                            subscription.setRoute(route);
                            subscriptions.add(subscription);
                        }

                        // Persist the subscriptions
                        device.setSubscriptions(subscriptions);
                        mDeviceDao.saveDevice(device);

                        return SubscriptionResult.OK;
                    }
                }
            }

            return SubscriptionResult.BAD_SUBSCRIPTION_REQUEST;
        });
    }

    /**
     * Sanity check the data we have received from the client.
     *
     * @param requestBody the body of the incoming request.
     * @return enum success result type.
     */
    private boolean validateInputData(Http.RequestBody requestBody) {
        if (requestBody != null) {
            Map<String, String[]> formEncodedMap = requestBody.asFormUrlEncoded();

            // Check that there was a valid list of routes and device uuid.
            if (formEncodedMap.containsKey(DEVICE_UUID_KEY) && formEncodedMap.containsKey(ROUTE_LIST_KEY)) {
                return true;
            }
        }
        return false;
    }
}
