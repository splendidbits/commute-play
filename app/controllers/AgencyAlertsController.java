package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import dao.AgencyDao;
import main.Constants;
import models.alerts.Agency;
import models.alerts.Route;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import services.AgencyManager;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AgencyAlertsController extends Controller {
    private static final String SEPTA_RAW_JSON_FEED = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php";
    private static final String INAPP_RAW_JSON_FEED = String.format(Locale.US, "%s/alerts/inapp", Constants.API_SERVER_HOST);

    @Inject
    private AgencyDao mAgencyDao;

    @Inject
    private WSClient mWSClient;

    @Inject
    private AgencyManager mAgencyManager;

    public Result index() {
        return ok();
    }

    /**
     * Get raw alerts by proxying the SEPTA alerts feed. Always makes a live query without
     * hitting cache. Should be a private API.
     *
     * @return Raw Agency alerts feed (json, xml, etc) wrapped in a CompletionStage.
     */
    public CompletionStage<Result> fetchRawAgencyAlerts(@Nullable Integer agencyId) {
        if (agencyId == null) {
            return CompletableFuture.completedFuture(badRequest());
        }

        CompletionStage<WSResponse> agencyDownloadStage;
        switch (agencyId) {
            case 1:
                String route = request().getQueryString("req1");
                agencyDownloadStage = mWSClient
                        .url(SEPTA_RAW_JSON_FEED)
                        .setFollowRedirects(true)
                        .setQueryString(route != null ? String.format(Locale.US, "req1=%s", route) : "all")
                        .get();
                break;

            case 2:
                agencyDownloadStage = mWSClient
                        .url(INAPP_RAW_JSON_FEED)
                        .setFollowRedirects(true)
                        .get();
                break;

            default:
                return CompletableFuture.completedFuture(badRequest());
        }

        return agencyDownloadStage.thenApply(response -> {

            if (response.getStatus() == 200 && response.getBody() != null) {
                return ok(response.getBody()).as("application/json");
            }
            return badRequest();
        });
    }

    /**
     * Fetches an {@link Agency} and all subsequent Routes, Alerts, and Locations for that Agency.
     * 1) Checks agency cache to try and quickly retrieve the data.
     * 2) If the cache misses, Make a request to the database to retrieve Agencies.
     *
     * @param agencyId id of the agency to return.
     * @return Entire agency in json format.
     */
    public CompletionStage<Result> getAgencyAlerts(int agencyId) {
        // Check the cache.
        Agency agency = mAgencyManager.getCachedAgency(agencyId);

        // If the agency was missing or expired, fetch from the database.
        if (agency == null) {
            agency = mAgencyDao.getAgency(agencyId);
        }

        return agency != null
                ? CompletableFuture.completedFuture(ok(Json.toJson(agency)))
                : CompletableFuture.completedFuture(ok(Json.newObject()));
    }

    /**
     * Get the complete list of agencies saved.
     *
     * @return collection of {@link Agency}'s.
     */
    public CompletableFuture<Result> getAgencies() {
        // Check the cache.
        List<Agency> agencies = mAgencyManager.getCachedAgencyMetadata();
        JsonNode jsonAgencies = Json.toJson(agencies);

        // Delete all nodes with children (it will just delete "routes" from the agencies
        // as that is the only Agency array, then breaks to next Agency.
        for (JsonNode jsonAgency : jsonAgencies) {
            for (Iterator<JsonNode> it = jsonAgency.elements(); it.hasNext(); ) {
                JsonNode jsonAgencyNode = it.next();
                if (jsonAgencyNode.isArray()) {
                    it.remove();
                    break;
                }
            }
        }

        return CompletableFuture.completedFuture(ok(jsonAgencies.toString()));
    }

    /**
     * Get a collection of saved alerts for a given agencyId and
     * routeName (non primary key)
     *
     * @param agencyId agencyId for route.
     * @param routeId  routeName for route.
     * @return Collection of matched alerts.
     */
    public CompletableFuture<Result> getRouteAlerts(Integer agencyId, String routeId) {
        if (agencyId != null && routeId != null) {
            // Check the agency cache for a valid route.
            Agency agency = mAgencyManager.getCachedAgency(agencyId);
            if (agency != null && agency.routes != null) {
                for (Route agencyRoute : agency.routes) {
                    if (agencyRoute.routeId.equals(routeId)) {
                        return CompletableFuture.completedFuture(ok(Json.toJson(agencyRoute)));
                    }
                }
            }

            Route route = mAgencyDao.getRoute(agencyId, routeId);
            return CompletableFuture.completedFuture(ok(Json.toJson(route)));
        }

        return CompletableFuture.completedFuture(ok(Json.newObject()));
    }
}
