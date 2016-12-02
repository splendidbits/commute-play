package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import dao.AgencyDao;
import helpers.RequestHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class AgencyAlertsController extends Controller {
    private static final String SEPTA_RAW_JSON_FEED = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php";

    @Inject
    private AgencyDao mAgencyDao;

    @Inject
    private WSClient mWSClient;

    public Result index() {
        return ok();
    }

    /**
     * Get raw alerts by proxying the SEPTA alerts feed;
     *
     * @return json SEPTA alerts in json format
     */
    public CompletionStage<Result> getRawAgencyAlerts(int agencyId) {
        final String ROUTE_QUERY_ATTR = "req1";
        CompletionStage<WSResponse> agencyFeedDownloadStage = new CompletableFuture<>();

        Function<WSResponse, Result> checkResponseFunc = response -> {
            String responseBody = response.getBody();
            if (response.getStatus() == 200 && responseBody != null && !responseBody.isEmpty()) {
                return ok(responseBody).as("application/json");
            }
            return badRequest();
        };

        Map<String, String[]> requestQueries = request().queryString();
        if (requestQueries != null && !requestQueries.isEmpty() && requestQueries.containsKey(ROUTE_QUERY_ATTR)) {
            agencyFeedDownloadStage = mWSClient.url(SEPTA_RAW_JSON_FEED)
                    .setFollowRedirects(true)
                    .setQueryParameter(ROUTE_QUERY_ATTR, requestQueries.get(ROUTE_QUERY_ATTR)[0])
                    .get();
        } else {
            return CompletableFuture.completedFuture(badRequest());
        }

        return agencyFeedDownloadStage.thenApply(checkResponseFunc);
    }

    /**
     * Get the complete list of agencies saved.
     *
     * @return collection of {@link Agency}'s.
     */
    public Result getAgencies() {
        List<Agency> agencies = mAgencyDao.getAgencies();
        JsonNode jsonArray = Json.toJson(agencies);
        RequestHelper.removeJsonArrayElement(jsonArray, "routes");

        return ok(jsonArray);
    }

    /**
     * Get a collection of saved routes for a given agencyId.
     *
     * @param agencyId agencyId for route.
     * @return Collection of matched routes.
     */
    public Result getRoutesForAgency(int agencyId) {
        List<Route> routes = mAgencyDao.getRoutes(agencyId);
        JsonNode jsonArray = Json.toJson(routes);
        RequestHelper.removeJsonArrayElement(jsonArray, "alerts");

        return ok(jsonArray);
    }

    /**
     * Get a collection of saved routes for a given agencyId and
     * routeName (non primary key)
     *
     * @param agencyId agencyId for route.
     * @param routeId  routeId for route.
     * @return Collection of matched routes.
     */
    public Result getRoutesForAgencyRoute(int agencyId, @Nonnull String routeId) {
        List<Route> routes = mAgencyDao.getRoutes(agencyId, Collections.singletonList(routeId));

        return ok(Json.toJson(routes));
    }

    /**
     * Get a collection of saved alerts for a given agencyId.
     *
     * @param agencyId agencyId for route.
     * @return Collection of matched alerts.
     */
    public Result getAlertsForRoute(int agencyId, String routeId) {
        List<Route> routes = mAgencyDao.getRoutes(agencyId);
        List<Alert> alerts = new ArrayList<>();

        if (routes != null) {
            for (Route route : routes) {
                if (route.alerts != null && !route.alerts.isEmpty()) {
                    alerts.addAll(route.alerts);
                }
            }
        }

        return ok(Json.toJson(alerts));
    }

    /**
     * Get a collection of saved alerts for a given agencyId and
     * routeName (non primary key)
     *
     * @param agencyId agencyId for route.
     * @param routeId  routeName for route.
     * @return Collection of matched alerts.
     */
    public Result getAlertsForAgencyRoute(int agencyId, String routeId) {
        List<Route> routes = mAgencyDao.getRoutes(agencyId, Collections.singletonList(routeId));
        List<Alert> alerts = new ArrayList<>();

        for (Route route : routes) {
            if (route.alerts != null && !route.alerts.isEmpty()) {
                alerts.addAll(route.alerts);
            }
        }

        return ok(Json.toJson(alerts));
    }
}
