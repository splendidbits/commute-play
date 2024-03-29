package serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import agency.InAppMessageUpdate;
import enums.AlertType;
import main.Constants;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.Logger;

/**
 * Gson SEPTA Alerts Deserializer. Convert the SEPTA www3 alerts feed into
 * the commute GCM agency alerts bundle models.
 */
public class InAppMessagesDeserializer implements JsonDeserializer<Agency> {
    private static final TimeZone timezone = TimeZone.getTimeZone("UTC");
    private Agency mAgency;

    public InAppMessagesDeserializer() {
        mAgency = new Agency(InAppMessageUpdate.AGENCY_ID);
        mAgency.setName(InAppMessageUpdate.AGENCY_NAME);
        mAgency.setPhone("+1 (555) 867 5309");
        mAgency.setUtcOffset(-8f);
        mAgency.setRoutes(new ArrayList<>());
        mAgency.setExternalUri(String.format(Locale.US, "%s/alerts/v1/agency/2", Constants.PROD_API_SERVER_HOST));
    }

    @Override
    public Agency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Logger.info("Started parsing inapp messages json body");

        // The InApp message alerts feed uses DataFormat: 2016/10/10 3:52pm (yyyy/mm/dd hh mma)
        SimpleDateFormat messageTimeFormat = new SimpleDateFormat("yyyy/mm/dd hh:mma", Locale.US);
        messageTimeFormat.setLenient(true);

        try {
            final JsonArray schedulesArray = json.getAsJsonArray();
            if (schedulesArray != null) {

                // Iterate through each object in the alerts document.
                List<Alert> alerts = new ArrayList<>();
                for (JsonElement scheduleRow : schedulesArray) {
                    JsonObject bucket = scheduleRow.getAsJsonObject();

                    String messageTitle = bucket.get("message_title").getAsString();
                    String messageSubtitle = bucket.get("message_subtitle").getAsString();
                    String messageBody = bucket.get("message_body").getAsString();
                    String lastUpdated = bucket.get("last_updated").getAsString();
                    boolean isHighPriority = bucket.get("high_priority").getAsBoolean();

                    Calendar lastUpdateCalendar = Calendar.getInstance(timezone, Locale.US);
                    if (lastUpdated != null && !lastUpdated.isEmpty()) {
                        lastUpdateCalendar.setTime(messageTimeFormat.parse(lastUpdated));
                    }

                    AlertType type = AlertType.TYPE_IN_APP;
                    Alert alert = new Alert();
                    alert.setType(type);
                    alert.setMessageTitle(messageTitle);
                    alert.setMessageSubtitle(messageSubtitle);
                    alert.setMessageBody(messageBody);
                    alert.setHighPriority(isHighPriority);
                    alert.setLastUpdated(lastUpdateCalendar);
                    alerts.add(alert);
                }

                if (!alerts.isEmpty()) {
                    Route route = new Route();
                    route.setRouteId(InAppMessageUpdate.ROUTE_ID);
                    route.setRouteName(InAppMessageUpdate.ROUTE_NAME);
                    route.setAlerts(alerts);
                    route.setSticky(true);
                    route.setDefault(true);

                    mAgency.setRoutes(Arrays.asList(route));
                }
            }

        } catch (IllegalStateException pe) {
            Logger.error("Error parsing json body into alert object", pe);

        } catch (ParseException e) {
            Logger.error("Error parsing json date(s) into alert object", e);
        }

        Collections.sort(mAgency.getRoutes());
        Logger.info("Finished creating and sorting in-app messages.");

        return mAgency;
    }
}
