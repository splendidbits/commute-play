package agencies.septa;

import com.avaje.ebean.annotation.Transactional;
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
import helpers.AgencyUpdate;

public class FetchSeptaAlerts extends Controller {
    public static final String SEPTA_ALERTS_JSON_URL = "http://localhost:8888/septa_instant/alerts.json";
//    public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

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
    @Transactional
    public void process() {

        WSResponse response;
        try {
            WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);
            request.setContentType("application/json");

            F.Promise<WSResponse> promiseOfResult = request.get();
            response = promiseOfResult.get(Constants.AGENCY_ALERTS_DOWNLOAD_MS); // is this blocked?

        } catch (Exception exception) {
            Log.e("Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
            return;
        }

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

        Log.d("Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");
        AgencyUpdate agencyUpdate = new AgencyUpdate();
        agencyUpdate.saveAndNotifyAgencySubscribers(agencyBundle);
    }
}
