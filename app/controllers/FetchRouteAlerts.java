package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
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
        currentAlert.currentMessage = "Until Further Notice<\\/p>\\n\\t\\t\\t\\t <p>Due to an unstable building adjacent to the Chester Transportation Center entrance passengers will board on Welsh St. (";
        currentAlert.lastUpdated = "03:18:14:647PM";

        Alert advisoryAlert = new Alert();
        advisoryAlert.advisoryMessage = "<div class=\\\"fares_container\\\">\\n\\t\\t\\t\\t <h3 class=\\\"separated\\\">Stop Discontinuation | Wynnewood Rd. & Lancaster Ave. (Westbound)<\\/h3>\\n\\t\\t\\t\\t <p class=\\\"desc separated\\\">Until Further Notice<\\/p>\\n\\t\\t\\t\\t <p>Due to construction, the Route 105 westbound transit stop at Wynnewood Rd. and Lancaster Ave. will be discontinued until further notice<\\/p>\\n<p>Customers should use the stop on Wynnewood Rd. at Penn Rd. (Wynnewood Station)<\\/p>\\n\\t\\t\\t <\\/div>\\n\\t\\t\\t <div class=\\\"fares_container\\\">\\n\\t\\t\\t\\t <h3 class=\\\"separated\\\">Temporary Stop Discontinuation<\\/h3>\\n\\t\\t\\t\\t <p class=\\\"desc separated\\\">Beginning Monday, March 30, 2015 - Until Further Notice<\\/p>\\n\\t\\t\\t\\t <p>Due to area construction, the Eastbound Transit stop for Routes 92, <strong>105<\\/strong>, and 106 on Lancaster Ave. and Valley Rd. will be discontinued until further notice<\\/p>\\n<p>Customers should use the next eastbound stop, located at Lancaster Ave. and Darby Rd.<\\/p>\\n\\t\\t\\t <\\/div>";
        advisoryAlert.lastUpdated = "03:48:14:647 PM";

        ArrayList<Alert> alerts = new ArrayList<>();
        alerts.add(currentAlert);
        alerts.add(advisoryAlert);

        Route route = new Route();
        route.routeId = "bus_route_105";
        route.routeName = "105";
        route.routeAlerts = alerts;

        ArrayList<Route> routes = new ArrayList<>();
        routes.add(route);

        Agency agency = new Agency();
        agency.id = 1;
        agency.agencyName = "SEPTA";
        agency.routes = routes;

        AlertsDatabaseService alda = AlertsDatabaseService.getInstance();
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
