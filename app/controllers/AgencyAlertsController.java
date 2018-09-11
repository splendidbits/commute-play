package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.annotation.Nullable;

import agency.InAppMessageUpdate;
import agency.SeptaAgencyUpdate;
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

public class AgencyAlertsController extends Controller {
    // TODO Remove
    // private static final String SEPTA_RAW_JSON_FEED = "http://localhost:8181/alerts/v1/agency/1/raw";
    private static final String SEPTA_RAW_JSON_FEED = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php";
    private static final String INAPP_RAW_JSON_FEED = String.format(Locale.US,
            "%s/alerts/inapp", Constants.PROD_API_SERVER_HOST);

    private AgencyDao mAgencyDao;
    private WSClient mWSClient;
    private AgencyManager mAgencyManager;

    @Inject
    public AgencyAlertsController(AgencyDao mAgencyDao, WSClient mWSClient, AgencyManager mAgencyManager) {
        this.mAgencyDao = mAgencyDao;
        this.mWSClient = mWSClient;
        this.mAgencyManager = mAgencyManager;
    }

    public Result index() {
        return ok();
    }

    /**
     * Get raw alerts by proxying the SEPTA alerts feed. Always makes a live query without
     * hitting cache. Should be a private API.
     *
     * @return Raw Agency alerts feed (json, xml, etc) wrapped in a CompletionStage.
     */
    public CompletionStage<Result> fetchRawAgencyAlerts(@Nullable String agencyId) {
        if (agencyId == null) {
            return CompletableFuture.completedFuture(badRequest());
        }

        CompletionStage<WSResponse> agencyDownloadStage = null;
        if (SeptaAgencyUpdate.AGENCY_ID.equals(agencyId)) {
            String route = request().getQueryString("req1");
            agencyDownloadStage = mWSClient
                    .url(SEPTA_RAW_JSON_FEED)
                    .setFollowRedirects(true)
                    .setQueryString(route != null ? String.format(Locale.US, "req1=%s", route) : "all")
                    .get();

        } else if (InAppMessageUpdate.AGENCY_ID.equals(agencyId)) {
                agencyDownloadStage = mWSClient
                        .url(INAPP_RAW_JSON_FEED)
                        .setFollowRedirects(true)
                        .get();
        }

        if (agencyDownloadStage == null) {
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
    public CompletionStage<Result> getAgencyAlerts(String agencyId) {
        return CompletableFuture.supplyAsync(() -> {
            Agency agency = mAgencyManager.getCachedAgency(agencyId);
            if (agency == null) {
                agency = mAgencyDao.getAgency(agencyId);
            }
            return agency;

        }).thenApply(agency -> agency != null
                ? ok(Json.toJson(agency))
                : ok(Json.newObject()));
    }

    /**
     * Get the complete list of agencies saved.
     *
     * @return collection of {@link Agency}'s.
     */
    public CompletableFuture<Result> getAgencies() {
        return CompletableFuture.supplyAsync(() -> {
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

            return ok(jsonAgencies.toString());
        });
    }

    /**
     * Get a collection of saved alerts for a given agencyId and
     * routeName (non primary key)
     *
     * @param agencyId agencyId for route.
     * @param routeId  routeName for route.
     * @return Collection of matched alerts.
     */
    public CompletableFuture<Result> getRouteAlerts(String agencyId, String routeId) {
        return CompletableFuture.supplyAsync(() -> {
            if (agencyId != null && routeId != null) {

                // Check the agency cache for a valid route.
                Agency agency = mAgencyManager.getCachedAgency(agencyId);
                if (agency != null && agency.getRoutes() != null) {
                    for (Route agencyRoute : agency.getRoutes()) {
                        if (agencyRoute.getRouteId().equals(routeId)) {
                            return ok(Json.toJson(agencyRoute));
                        }
                    }
                }

                Route route = mAgencyDao.getRoute(agencyId, routeId);
                return ok(Json.toJson(route));
            }

            return badRequest(Json.newObject());
        });
    }
}
