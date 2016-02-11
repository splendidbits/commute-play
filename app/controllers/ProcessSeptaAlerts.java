package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import main.Constants;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

public class ProcessSeptaAlerts extends Controller {
    public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";
    public static final String AGENCY_NAME = "SEPTA";

    public Result index() {
        WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);
        F.Promise<WSResponse> promiseOfResult = request.get();

        WSResponse response = promiseOfResult.get(Constants.AGENCY_ALERTS_DOWNLOAD_MS); //block here
        JsonNode jsonData =  response.asJson();

//        AgencyRouteAlerts route = new AgencyRouteAlerts();
//        route.agencyName = AGENCY_NAME;

//        List<JsonNode> allRouteAlerts = jsonData.findParents("route_id");

//        Download current alerts.
//        1: Download agency alerts.
//        2: Go through each Route > Alert bundle and find any differences
//        3: Collect the new alerts

//        Send to GCM processor
//        4: Persist new data
//        5: Get list of subscriptions for route
//        6: send data in batches of 1000 to google.

        return ok("Client:"+jsonData);
//        return ok();
    }
}
