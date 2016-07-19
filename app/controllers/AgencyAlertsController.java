package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import dao.AgencyDao;
import helpers.RequestHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgencyAlertsController extends Controller {

    @Inject
    private AgencyDao mAgencyDao;

    public Result index() {
        return ok();
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
    public Result getAlertsForAgency(int agencyId) {
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
