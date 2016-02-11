package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Agency;
import models.Alert;
import models.Route;
import play.Logger;
import play.db.ebean.Transactional;
import play.libs.F;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import services.AlertsDatabaseService;

import javax.inject.Inject;
import java.util.ArrayList;

/**
 * Download and store route alerts for various providers.
 */
public class FetchRouteAlerts extends Controller {
    @Inject WSClient ws;

    /**
     * Blocking Request - Result of alerts agency json file.
     */
    @Transactional
    public Result downloadSeptaAlerts() {

        Alert currentAlert = new Alert();
        currentAlert.currentMessage = "this is a messagee";
        currentAlert.lastUpdated = "now";

        Alert advisoryAlert = new Alert();
        advisoryAlert.advisoryMessage = "this is an advisory";
        advisoryAlert.lastUpdated = "then";

        ArrayList<Alert> alerts = new ArrayList<>();
        alerts.add(currentAlert);
        alerts.add(advisoryAlert);

        Route route = new Route();
        route.routeId = "bus_route_22";
        route.routeName = "22";
        route.routeAlerts = alerts;

        ArrayList<Route> routes = new ArrayList<>();
        routes.add(route);

        Agency agency = new Agency();
        agency.agencyName = "SEPTA";
        agency.routes = routes;

        AlertsDatabaseService alda = new AlertsDatabaseService();
        alda.saveRouteAlerts(agency);

        return ok();

//        WSRequest request = WS.url("http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all");
//        F.Promise<WSResponse> promiseOfResult = request.get();
//
//        WSResponse response = promiseOfResult.get(Constants.AGENCY_ALERTS_DOWNLOAD_MS); //block here
//
//        String jsonData =  response.getBody();
//        return ok("Client:"+jsonData);
    }

    // Testing. TODO: Remove.
    final F.Function<WSResponse, Result> alertsJsonResult = new F.Function<WSResponse, Result>() {
        @Override
        public Result apply(WSResponse response) throws Throwable {
            JsonNode alertsJson = response.asJson();
            Logger.debug(alertsJson.asText());

            return forbidden();
        }
    };

}
