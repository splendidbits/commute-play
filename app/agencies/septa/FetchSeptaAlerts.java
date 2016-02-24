package agencies.septa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.SeptaDeserializer;
import main.Constants;
import main.Log;
import models.alerts.Agency;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import services.AlertsDatabaseService;

public class FetchSeptaAlerts extends Controller {
    public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";
    public static final String AGENCY_NAME = "SEPTA";

    /**
     * Download SEPTA alerts from the json server and send them to the
     * dispatch processes.
     *
     * Download current alerts.
     * 1: Download agency alerts.
     * 2: Bundle into standard format.
     *
     * Send to GCM processor
     *
     * 2.5: Go through each Route > Alert bundle and find any differences
     * 3: Collect the new alerts
     * 4: Persist new data
     * 5: Get list of subscriptions for route
     * 6: send data in batches of 1000 to google.
     *
     */
    public static void process() {
        WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);
        F.Promise<WSResponse> promiseOfResult = request.get();

        request.setContentType("application/json");

        WSResponse response = promiseOfResult.get(Constants.AGENCY_ALERTS_DOWNLOAD_MS); // is this blocked?
        if (response.getStatus() != 200) {
            Log.e("Fetching SEPTA alerts json failed with error " + response.getStatus());
            return;
        }

        Log.d("Completed fetching SEPTA alerts");
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Agency.class, new SeptaDeserializer());
        final Gson gson = gsonBuilder.create();

        Agency agencyBundle = gson.fromJson(response.getBody(), Agency.class);
        Log.d("Started to parsing SEPTA alerts json body");

        AlertsDatabaseService alertsService = AlertsDatabaseService.getInstance();
        alertsService.saveRouteAlerts(agencyBundle);
        Log.d("Finished parsing SEPTA alerts json body");
    }
}
