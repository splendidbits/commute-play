package serializers;

import com.google.gson.*;
import main.Log;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gson SEPTA Alerts Deserializer. Convert the SEPTA www3 alerts feed into
 * the commute GCM agency alerts bundle models.
 */
public class SeptaAlertsDeserializer implements JsonDeserializer<Agency> {
    private static final String TAG = SeptaAlertsDeserializer.class.getSimpleName();

    private static final String AGENCY_NAME = "septa";
    private static final TimeZone timezone = TimeZone.getTimeZone("UTC");

    private Log mLog;
    public SeptaAlertsDeserializer(Log log) {
        mLog = log;
    }

    @Override
    public Agency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        mLog.d(TAG, "Started parsing SEPTA alerts json body");

        // The SEPTA alerts feed uses different date formats depending on the field
        // last_updated - Feb 20 2016 07:27:42:520PM
        SimpleDateFormat lastUpdatedDateFormat = new SimpleDateFormat("MMM dd yyyy hh:mm:ss:SSSa", Locale.US);
        lastUpdatedDateFormat.setLenient(true);

        // detour_%     - 1/14/2016   9:26 AM
        SimpleDateFormat detourDateFormat = new SimpleDateFormat("mm/dd/yyyy hh:mm a", Locale.US);
        detourDateFormat.setLenient(true);

        HashMap<String, Route> routeMap = new HashMap<>();
        try{
            final JsonArray schedulesArray = json.getAsJsonArray();

            if (schedulesArray != null){
                for (JsonElement scheduleRow : schedulesArray){

                    JsonObject bucket = scheduleRow.getAsJsonObject();
                    String routeId = bucket.get("route_id").getAsString().toLowerCase();
                    String routeName = bucket.get("route_name").getAsString();
                    String advisoryMessage = bucket.get("advisory_message").getAsString();
                    String currentMessage = bucket.get("current_message").getAsString();
                    String detourStartDate = bucket.get("detour_start_date_time").getAsString();
                    String detourEndDate = bucket.get("detour_end_date_time").getAsString();
                    String detourMessage = bucket.get("detour_message").getAsString();
                    String detourReason = bucket.get("detour_reason").getAsString();
                    String detourStartLocation = bucket.get("detour_start_location").getAsString();
                    Boolean isSnow = bucket.get("isSnow").getAsBoolean();
                    String lastUpdated = bucket.get("last_updated").getAsString();

                    if (routeId.isEmpty()) {
                        continue;
                    }

                    Alert alert = new Alert();
                    alert.advisoryMessage = advisoryMessage;
                    alert.currentMessage = currentMessage;
                    alert.detourMessage = detourMessage;
                    alert.detourReason = detourReason;
                    alert.detourStartLocation = detourStartLocation;
                    alert.isSnow = isSnow;

                    if (detourStartDate != null && !detourStartDate.isEmpty()) {
                        Calendar detourStartCal = Calendar.getInstance(timezone, Locale.US);
                        detourStartCal.setTime(detourDateFormat.parse(detourStartDate));
                        alert.detourStartDate = detourStartCal;
                    }

                    if (detourEndDate != null && !detourEndDate.isEmpty()) {
                        Calendar detourEndCal = Calendar.getInstance(timezone, Locale.US);
                        detourEndCal.setTime(detourDateFormat.parse(detourEndDate));
                        alert.detourEndDate = detourEndCal;
                    }

                    if (lastUpdated != null && !lastUpdated.isEmpty()) {
                        Calendar lastUpdateCal = Calendar.getInstance(timezone, Locale.US);
                        lastUpdateCal.setTime(lastUpdatedDateFormat.parse(lastUpdated));
                        alert.lastUpdated = lastUpdateCal;
                    }

                    // Move the alert to the corresponding value in the map.
                    Route route;
                    if (routeMap.containsKey(routeId)) {
                        route = routeMap.get(routeId);
                    } else {
                        route = new Route(routeId, routeName);
                        route.alerts = new ArrayList<>();
                    }

                    alert.route = route;
                    route.alerts.add(alert);
                    Collections.sort(route.alerts);
                    routeMap.put(routeId, route);
                }
            }
            mLog.d(TAG, "Finished creating SEPTA route-alert map.");

        }catch (IllegalStateException pe){
            mLog.c(TAG, "Error parsing json body into alert object", pe);

        } catch (ParseException e) {
            mLog.c(TAG, "Error parsing json date(s) into alert object", e);
        }

        // Create agency.
        Agency agency = new Agency();
        agency.agencyName = AGENCY_NAME;
        agency.agencyId = 1;
        agency.routes = new ArrayList<>();

        // Iterate through the collection of routes and add the alerts and route to the agency routes.
        for (Route route : routeMap.values()) agency.routes.add(route);

        return agency;
    }
}
