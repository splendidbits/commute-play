package serializers;

import agency.inapp.InAppMessageUpdate;
import com.google.gson.*;
import enums.AlertType;
import main.Constants;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.fluffylog.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gson SEPTA Alerts Deserializer. Convert the SEPTA www3 alerts feed into
 * the commute GCM agency alerts bundle models.
 */
public class InAppMessagesDeserializer implements JsonDeserializer<Agency> {
    private static final TimeZone timezone = TimeZone.getTimeZone("UTC");
    private Agency mAgency;

    public InAppMessagesDeserializer(@Nullable Agency partialAgency) {
        mAgency = partialAgency;

        // Create agency if there was no partially filled agency from the client.
        if (mAgency == null) {
            mAgency.name = InAppMessageUpdate.AGENCY_NAME;
            mAgency.phone = "+1 (555) 867 5309";
            mAgency.utcOffset = -8f;
            mAgency.routes = new ArrayList<>();
            mAgency.externalUri = String.format(Locale.US, "%s/alerts/v1/agency/2", Constants.PROD_API_SERVER_HOST);
        }

        mAgency = new Agency(InAppMessageUpdate.AGENCY_ID);
    }

    @Override
    public Agency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Logger.debug("Started parsing inapp messages json body");

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
                    alert.type = type;
                    alert.messageTitle = messageTitle;
                    alert.messageSubtitle = messageSubtitle;
                    alert.messageBody = messageBody;
                    alert.highPriority = isHighPriority;
                    alert.lastUpdated = lastUpdateCalendar;
                    alerts.add(alert);
                }

                if (!alerts.isEmpty()) {
                    Route route = new Route(InAppMessageUpdate.ROUTE_ID);
                    route.routeId = InAppMessageUpdate.ROUTE_ID;
                    route.routeName = InAppMessageUpdate.ROUTE_NAME;
                    route.agency = mAgency;
                    route.alerts = alerts;
                    route.isSticky = true;
                    route.isDefault = true;

                    mAgency.routes = Arrays.asList(route);
                }
            }

        } catch (IllegalStateException pe) {
            Logger.error("Error parsing json body into alert object", pe);

        } catch (ParseException e) {
            Logger.error("Error parsing json date(s) into alert object", e);
        }

        Collections.sort(mAgency.routes);
        Logger.debug("Finished creating and sorting in-app messages.");

        return mAgency;
    }
}
