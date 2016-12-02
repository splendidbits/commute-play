package agency.septa;

import agency.AgencyUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.AgencyDao;
import main.Constants;
import models.alerts.Agency;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.SeptaAlertsDeserializer;
import services.AlertsUpdateManager;
import services.PushMessageManager;
import services.fluffylog.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Agency updater for SEPTA alerts.
 */
@Singleton
public class SeptaAgencyUpdate extends AgencyUpdate {
    public static final int AGENCY_ID = 1;
    public static String AGENCY_NAME = "South East Pennsylvania Transit Association";
    private static final String APP_ALERT_URL = String.format(Locale.US,
            "%s/alerts/v1/agency/%d/raw?req1=all", Constants.API_SERVER_HOST, AGENCY_ID);
    private final ParseAgencyFunction mParseMessagesFunc;

    @Inject
    public SeptaAgencyUpdate(WSClient wsClient, AlertsUpdateManager alertsUpdateManager, AgencyDao agencyDao,
                             PushMessageManager pushMessageManager) {
        super(wsClient, alertsUpdateManager, agencyDao, pushMessageManager);

        mParseMessagesFunc = new ParseAgencyFunction();
    }

    @Override
    public void startAgencyUpdate() {
        try {
            Logger.debug("Starting download of SEPTA agency alert data.");
            // Proxy pass-through to http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all
            CompletionStage<WSResponse> downloadStage = mWsClient
                    .url(APP_ALERT_URL)
                    .setRequestTimeout(AGENCY_DOWNLOAD_TIMEOUT_MS)
                    .setFollowRedirects(true)
                    .get();

            downloadStage.thenApply(mParseMessagesFunc);

        } catch (Exception exception) {
            Logger.error("Error downloading agency data from " + APP_ALERT_URL, exception);
        }
    }

    /**
     * Parses and updates all downloaded Json Data
     */
    private class ParseAgencyFunction implements Function<WSResponse, Agency> {
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
                processAgencyUpdate(agencyAlerts);
            }
            return agencyAlerts;
        }
    }
}

