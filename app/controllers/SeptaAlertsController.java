package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import helpers.SeptaAlertsDeserializer;
import main.AgencyUpdate;
import main.Log;
import models.alerts.Agency;
import play.db.ebean.Transactional;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class SeptaAlertsController extends AgencyController {
    private static final String TAG = Application.class.getSimpleName();

    public static final String SEPTA_ALERTS_JSON_URL = "http://localhost:9000/assets/resources/alerts.json";
//  public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

    /**
     * Download SEPTA alerts from the json server and send them to the
     * dispatch processes.
     * <p>
     * Download current alerts.
     * 1: Download agency alerts.
     * 2: Bundle into standard format.
     * <p>
     * Send to GCM processor
     * <p>
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
        try {
            WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);

            CompletionStage<WSResponse> resultPromise = request.get();
            resultPromise.thenRun(new Runnable() {
                @Override
                public void run() {
                    updateAgencyData();
                }
            });

        } catch (Exception exception) {
            Log.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }
        return ok();
    }

    @Override
    public void updateAgency() {
        try {
            WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);

            CompletionStage<WSResponse> resultPromise = request.get();
            resultPromise.thenRun(new Runnable() {
                @Override
                public void run() {
                    updateAgencyData();
                }
            });

        } catch (Exception exception) {
            Log.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }
    }

    /**
     * Updates all septa agency data from the septa endpoint.
     */
    private synchronized void updateAgencyData() {
        Log.d(TAG, "Downloading SEPTA alerts");
        WSResponse response = null;

        try {
            WSRequest request = WS.url(SEPTA_ALERTS_JSON_URL);
            request.get();

            Log.d(TAG, "Downloaded SEPTA alerts");
            final Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer())
                    .create();

            Agency agencyBundle = gson.fromJson(response.getBody(), Agency.class);
            Log.d(TAG, "Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");

            AgencyUpdate agencyUpdate = new AgencyUpdate();
            agencyUpdate.saveAndNotifyAgencySubscribers(agencyBundle);

        } catch (Exception exception) {
            Log.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }

        if (response == null || response.getStatus() != 200) {
            Log.c(TAG, "Response from SEPTA alerts json was null");
        }
    }
}
