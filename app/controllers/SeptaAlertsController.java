package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.AgencyUpdate;
import helpers.SeptaAlertsDeserializer;
import main.Constants;
import main.Log;
import models.alerts.Agency;
import play.db.ebean.Transactional;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

public class SeptaAlertsController extends Controller {
    private static final String TAG = Application.class.getSimpleName();

    public static final String SEPTA_ALERTS_JSON_URL = "http://localhost:9000/assets/resources/alerts.json";
//  public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

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
     * @return Result.
     */
    @Transactional
    public Result downloadAlerts() {
        WSResponse response;
        try {
            WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);
            request.setContentType("application/json");

            F.Promise<WSResponse> promiseOfResult = request.get();
            response = promiseOfResult.get(Constants.AGENCY_ALERTS_DOWNLOAD_MS); // is this blocked?

        } catch (Exception exception) {
            Log.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
            return internalServerError();
        }

        if (response.getStatus() != 200) {
            Log.e(TAG, "Fetching SEPTA alerts json failed with error " + response.getStatus());
            return internalServerError();
        }

        Log.d(TAG, "Completed fetching SEPTA alerts");
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer())
                .create();

        Agency agencyBundle = gson.fromJson(response.getBody(), Agency.class);
        Log.d(TAG, "Started to parsing SEPTA alerts json body");

        Log.d(TAG, "Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");
        AgencyUpdate agencyUpdate = new AgencyUpdate();
        agencyUpdate.saveAndNotifyAgencySubscribers(agencyBundle);

        return ok();
    }
}
