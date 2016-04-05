package agency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controllers.Application;
import helpers.SeptaAlertsDeserializer;
import main.AlertsUpdateManager;
import main.Log;
import models.alerts.Agency;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

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
@Singleton
public class SeptaAgencyUpdate implements AgencyUpdate {

    private static final String TAG = Application.class.getSimpleName();

    public static final String SEPTA_ALERTS_JSON_URL = "http://localhost:9000/assets/resources/alerts.json";
//  public static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

    @Inject
    private WSClient mWsClient;

    @Inject
    private Log mLog;

    @Inject
    private AlertsUpdateManager mAlertsUpdateManager;

    @Inject
    public SeptaAgencyUpdate(WSClient wsClient, Log log, AlertsUpdateManager alertsUpdateManager) {
        mWsClient = wsClient;
        mLog = log;
        mAlertsUpdateManager = alertsUpdateManager;
    }

    @Override
    public void updateAgency() {
        try {
            mLog.d(TAG, "Starting download of SEPTA agency alert data.");
            WSRequest request = mWsClient
                    .url(SEPTA_ALERTS_JSON_URL)
                    .setRequestTimeout(AGENCY_DOWNLOAD_TIMEOUT_MS)
                    .setFollowRedirects(true);

            CompletionStage<WSResponse> resultPromise = request.get();
            resultPromise.whenComplete(new BiConsumer<WSResponse, Throwable>() {

                @Override
                public void accept(WSResponse response, Throwable throwable) {
                    if (throwable != null) {
                        mLog.e(TAG, "Error fetching SEPTA alerts resource", throwable);
                    } else {
                        updateAgencyData(response);
                    }
                }
            });

        } catch (Exception exception) {
            mLog.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }
    }

    /**
     * Updates all septa agency data from the septa endpoint.
     */
    private CompletionStage<Boolean> updateAgencyData(@Nonnull WSResponse response) {
        try {
            mLog.d(TAG, "Downloaded SEPTA alerts");
            final Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer(mLog))
                    .create();

            Agency agencyBundle = gson.fromJson(response.getBody(), Agency.class);
            mLog.d(TAG, "Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");

            mAlertsUpdateManager.saveAndNotifyAgencySubscribers(agencyBundle);
            return CompletableFuture.completedFuture(true);

        } catch (Exception exception) {
            mLog.e(TAG, "Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }

        if (response.getStatus() != 200) {
            mLog.c(TAG, "Response from SEPTA alerts json was null");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }
}
