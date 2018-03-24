package agency.septa;

import agency.AgencyUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.Constants;
import models.alerts.Agency;
import play.Environment;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.SeptaAlertsDeserializer;
import services.AgencyManager;
import services.PushMessageManager;
import play.Logger;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Agency updater for SEPTA alerts.
 */
public class SeptaAgencyUpdate extends AgencyUpdate {
    public static final String AGENCY_NAME = "South East Pennsylvania Transit Association";
    public static final int AGENCY_ID = 1;

    private WSClient mWsClient;
    private Environment mEnvironment;

    @Inject
    public SeptaAgencyUpdate(WSClient wsClient, Environment environment, AgencyManager agencyManager, PushMessageManager pushMessageManager) {
        super(agencyManager, pushMessageManager);

        mWsClient = wsClient;
        mEnvironment = environment;
    }

    @Override
    public void startAgencyUpdate() {
        String septaAlertUrl = String.format(Locale.US, "%s/alerts/v1/agency/%d/raw?req1=all", Constants.PROD_API_SERVER_HOST, AGENCY_ID);

        try {
            Logger.debug("Starting download of SEPTA agency alert data.");

            // Proxy pass-through to http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=all
            CompletionStage<WSResponse> downloadStage = mWsClient
                    .url(septaAlertUrl)
                    .setRequestTimeout(AGENCY_DOWNLOAD_TIMEOUT_MS)
                    .setFollowRedirects(true)
                    .get();

            downloadStage.thenApply(new ParseAgencyFunction());

        } catch (Exception exception) {
            Logger.error("Error downloading agency data from " + septaAlertUrl, exception);
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

                // SEPTA alerts were empty.
                if (response.getBody() == null || response.getBody().isEmpty()) {
                    Logger.error("SEPTA JSON alerts body was empty");
                    return null;
                }

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

