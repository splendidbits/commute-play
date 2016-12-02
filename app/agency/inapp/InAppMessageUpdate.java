package agency.inapp;

import agency.AgencyUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.AgencyDao;
import main.Constants;
import models.alerts.Agency;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.InAppMessagesDeserializer;
import services.AlertsUpdateManager;
import services.PushMessageManager;
import services.fluffylog.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Agency updater for the in-app messages.
 */
@Singleton
public class InAppMessageUpdate extends AgencyUpdate {
    public static final int AGENCY_ID = 2;
    public static final String AGENCY_NAME = "commuteio";
    public static final String ROUTE_ID = "general";
    public static final String ROUTE_NAME = "General in-app";

    private static final int APP_ALERT_TIMEOUT = 1000 * 20;
    private static final String APP_ALERT_URL = String.format(Locale.US,
            "%s/alerts/v1/agency/%d/raw", Constants.API_SERVER_HOST, AGENCY_ID);
    private final ParseMessages mParseMessagesFunc;

    @Inject
    public InAppMessageUpdate(WSClient wsClient, AlertsUpdateManager alertsUpdateManager, AgencyDao agencyDao,
                              PushMessageManager pushMessageManager) {
        super(wsClient, alertsUpdateManager, agencyDao, pushMessageManager);

        mParseMessagesFunc = new ParseMessages();
    }

    @Override
    public void startAgencyUpdate() {
        try {
            Logger.debug("Starting compilation of Commute in-app Message alerts.");

            try {
                Logger.debug("Starting download of in-app messages.");
                CompletionStage<WSResponse> downloadStage = mWsClient
                        .url(APP_ALERT_URL)
                        .setRequestTimeout(APP_ALERT_TIMEOUT)
                        .setFollowRedirects(true)
                        .get();

                downloadStage.thenApply(mParseMessagesFunc);

            } catch (Exception exception) {
                Logger.error("Error downloading agency data from " + APP_ALERT_URL, exception);
            }

        } catch (Exception exception) {
            Logger.error("Error compiling in-app agency data ", exception);
        }
    }

    /**
     * Parses and updates all downloaded Json Data
     */
    private class ParseMessages implements Function<WSResponse, Agency> {
        @Override
        public Agency apply(WSResponse response) {
            Agency agencyAlerts = null;
            if (response != null && response.getStatus() == 200) {
                Logger.debug("Downloaded in-app Messages.");

                // Create gson serializer
                final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Agency.class, new InAppMessagesDeserializer(null))
                        .create();

                Logger.debug("Finished parsing in-app json body. Sending to AgencyUpdateService");
                agencyAlerts = gson.fromJson(response.getBody(), Agency.class);
                processAgencyUpdate(agencyAlerts);
            }
            return agencyAlerts;
        }
    }
}

