package agency.inapp;

import agency.AgencyUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.Constants;
import models.alerts.Agency;
import play.Environment;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.InAppMessagesDeserializer;
import services.AgencyManager;
import services.PushMessageManager;
import services.fluffylog.Logger;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Agency updater for the in-app messages.
 */
public class InAppMessageUpdate extends AgencyUpdate {
    private static final int APP_ALERT_TIMEOUT = 1000 * 20;

    public static final int AGENCY_ID = 2;
    public static final String AGENCY_NAME = "Commute App";
    public static final String ROUTE_ID = "commuteapp";
    public static final String ROUTE_NAME = "Commute App Messages";

    private ParseMessages mParseMessagesFunc;
    private WSClient mWsClient;
    private Environment mEnvironment;

    @Inject
    public InAppMessageUpdate(WSClient wsClient, Environment environment, AgencyManager agencyManager, PushMessageManager pushMessageManager) {
        super(agencyManager, pushMessageManager);

        mWsClient = wsClient;
        mEnvironment = environment;
        mParseMessagesFunc = new ParseMessages();
    }

    @Override
    public void startAgencyUpdate() {
        Logger.debug("Starting compilation of Commute App Message alerts.");
        String hostname = Constants.IS_DEBUG ? Constants.PROD_API_SERVER_HOST : Constants.DEBUG_API_SERVER_HOST;
        String alertUrl = String.format(Locale.US, "%s/alerts/v1/agency/%d/raw?req1=all", hostname, AGENCY_ID);

        try {

            Logger.debug("Starting download of in-app messages.");
            CompletionStage<WSResponse> downloadStage = mWsClient
                    .url(alertUrl)
                    .setRequestTimeout(APP_ALERT_TIMEOUT)
                    .setFollowRedirects(true)
                    .get();

            downloadStage.thenApply(mParseMessagesFunc);

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

