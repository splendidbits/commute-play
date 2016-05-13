package agency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controllers.Application;
import models.alerts.Agency;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.SeptaAlertsDeserializer;
import services.AlertsUpdateManager;
import services.splendidlog.Logger;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

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
 */
public class SeptaAgencyUpdate implements AgencyUpdate {

    private static final String TAG = Application.class.getSimpleName();
    private static final String SEPTA_ALERTS_JSON_URL = "http://localhost:9000/assets/resources/alerts.json";
//  public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

    @Inject
    private WSClient mWsClient;

    @Inject
    private AlertsUpdateManager mAlertsUpdateManager;

    @Inject
    public SeptaAgencyUpdate(WSClient wsClient, AlertsUpdateManager alertsUpdateManager) {
        mWsClient = wsClient;
        mAlertsUpdateManager = alertsUpdateManager;
    }

    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
    @Override
    public void updateAgency() {
        try {
            Logger.debug("Starting download of SEPTA agency alert data.");
            CompletionStage<WSResponse> resultPromise = mWsClient
                    .url(SEPTA_ALERTS_JSON_URL)
                    .setRequestTimeout(AGENCY_DOWNLOAD_TIMEOUT_MS)
                    .setFollowRedirects(true)
                    .get();

            resultPromise.thenAccept(new JsonDownloadConsumer());

        } catch (Exception exception) {
            Logger.error("Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }
    }

    /**
     * Promise result from agency json download.
     */
    private class JsonDownloadConsumer implements Consumer<WSResponse> {

        @Override
        public void accept(WSResponse response) {
            updateAgencyData(response);
        }
    }

    /**
     * Updates all septa agency data from the septa endpoint.
     */
    private CompletionStage<Boolean> updateAgencyData(WSResponse response) {
        if (response != null) {
            Agency agencyAlerts = null;

            if (response.getStatus() != 200) {
                Logger.error(String.format("Response status-code from SEPTA alerts endpoint was %d", response.getStatus()));
                return CompletableFuture.completedFuture(false);

            } else {
                Logger.debug("Downloaded SEPTA alerts");
            }

            try {
                final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer(null))
                        .create();
                agencyAlerts = gson.fromJson(response.getBody(), Agency.class);

            } catch (Exception exception) {
                Logger.error("Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
            }

            Logger.debug("Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");
            mAlertsUpdateManager.saveAndNotifyAgencySubscribers(agencyAlerts);
            return CompletableFuture.completedFuture(true);
        }

        Logger.error("SEPTA alerts response was null");
        return CompletableFuture.completedFuture(false);
    }
}
