package agency;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.inject.Inject;

import main.Constants;
import models.alerts.Agency;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.InAppMessagesDeserializer;
import services.AgencyManager;
import services.PushMessageManager;

/**
 * Agency updater for the in-app messages.
 */
public class InAppMessageUpdate extends AgencyUpdate {
    private static final int APP_ALERT_TIMEOUT = 1000 * 20;

    public static final String AGENCY_ID = "IN_APP";
    public static final String AGENCY_NAME = "Commute App Feed";
    public static final String ROUTE_ID = "commute";
    public static final String ROUTE_NAME = AGENCY_NAME;

    private WSClient mWsClient;

    @Inject
    public InAppMessageUpdate(WSClient wsClient, AgencyManager agencyManager, PushMessageManager pushMessageManager) {
        super(agencyManager, pushMessageManager);

        mWsClient = wsClient;
    }

    @Override
    public void startAgencyUpdate() {
        Logger.info("Starting compilation of Commute App Message alerts.");
        String alertUrl = String.format(Locale.US, "%s/alerts/v1/agency/%s/raw?req1=all", Constants.PROD_API_SERVER_HOST, AGENCY_ID);

        try {
            Logger.info("Starting download of in-app messages.");
            CompletionStage<WSResponse> downloadStage = mWsClient
                    .url(alertUrl)
                    .setRequestTimeout(APP_ALERT_TIMEOUT)
                    .setFollowRedirects(true)
                    .get();

            downloadStage.thenApply(new ParseMessages());

        } catch (Exception exception) {
            Logger.error("Error downloading agency data from " + alertUrl, exception);
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
                Logger.info("Downloaded in-app Messages.");

                // Create gson serializer
                final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Agency.class, new InAppMessagesDeserializer())
                        .create();

                Logger.info("Finished parsing in-app json body. Sending to AgencyUpdateService");
                agencyAlerts = gson.fromJson(response.getBody(), Agency.class);
                processAgencyUpdate(agencyAlerts);
            }
            return agencyAlerts;
        }
    }
}

