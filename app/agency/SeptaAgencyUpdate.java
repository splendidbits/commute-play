package agency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.SeptaAlertsDeserializer;
import services.AlertsUpdateManager;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

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

    private static final String SEPTA_ALERTS_JSON_URL = "http://localhost:9000/assets/resources/alerts.json";
//    private static final String SEPTA_ALERTS_JSON_URL = "http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all";

    private WSClient mWsClient;
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
            CompletionStage<WSResponse> downloadStage = mWsClient
                    .url(SEPTA_ALERTS_JSON_URL)
                    .setRequestTimeout(AGENCY_DOWNLOAD_TIMEOUT_MS)
                    .setFollowRedirects(true)
                    .get();

            downloadStage.thenApply(new ParseAgencyFunction());

        } catch (Exception exception) {
            Logger.error("Error downloading agency data from " + SEPTA_ALERTS_JSON_URL, exception);
        }
    }

    /**
     * Parses and updates all downloaded Json Data
     */
    private class ParseAgencyFunction implements Function<WSResponse, Agency> {

        /**
         * Modify the data in a series of route alerts to test things.
         *
         * @param agency agency bundle.
         */
        private void createLoadTestUpdates(@Nonnull Agency agency) {
            if (agency.routes != null) {
                Alert previousAlert = null;

                Collections.shuffle(agency.routes);
                for (Route route : agency.routes) {

                    Collections.shuffle(route.alerts);
                    for (Alert alert : route.alerts) {
                        alert.messageTitle = previousAlert != null
                                ? previousAlert.messageTitle
                                : alert.messageTitle;

                        alert.messageSubtitle = previousAlert != null
                                ? previousAlert.messageSubtitle
                                : alert.messageSubtitle;

                        alert.messageBody = previousAlert != null
                                ? previousAlert.messageBody
                                : alert.messageBody;

                        previousAlert = alert;
                    }
                }
            }
        }

        @Override
        public Agency apply(WSResponse response) {
            Agency agencyAlerts = null;
            if (response != null && response.getStatus() == 200) {
                Logger.debug("Downloaded SEPTA alerts");

                // Create gson serializer
                final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer(null))
                        .create();

                Logger.debug("Finished parsing SEPTA alerts json body. Sending to AgencyUpdateService");
                agencyAlerts = gson.fromJson(response.getBody(), Agency.class);

                // TODO: Comment to disable load test.
//                createLoadTestUpdates(agencyAlerts);

                mAlertsUpdateManager.processAgencyDownload(agencyAlerts);
            }
            return agencyAlerts;
        }
    }
}

