package helpers;

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
public class SeptaDeserializer implements JsonDeserializer<Agency> {
    private static final String AGENCY_NAME = "septa";
    private static final TimeZone timezone = TimeZone.getTimeZone("America/New_York");
    private static SimpleDateFormat lastUpdatedDateFormat;
    private static SimpleDateFormat detourDateFormat;

    public SeptaDeserializer() {
        // The SEPTA alerts feed uses different date formats depending on the field

        // last_updated - Feb 20 2016 07:27:42:520PM
        lastUpdatedDateFormat = new SimpleDateFormat("MMM dd yyyy hh:mm:ss:SSSa", Locale.US);
        // detour_%     - 1/14/2016   9:26 AM
        detourDateFormat = new SimpleDateFormat("mm/dd/yyyy hh:mm a", Locale.US);
        lastUpdatedDateFormat.setLenient(true);
        detourDateFormat.setLenient(true);
    }

    @Override
    public Agency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        List<Alert> alertList = new ArrayList<>();

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

                    Route route = new Route(routeId);
                    route.routeName = routeName;

                    Alert alert = new Alert();
                    alert.route = route;
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
                    alertList.add(alert);
                }
            }
            Log.d("Finished creating SEPTA route-alert map.");

        }catch (IllegalStateException pe){
            Log.c("Error parsing json body into alert object", pe);

        } catch (ParseException e) {
            Log.c("Error parsing json date(s) into alert object", e);
        }

        // Loop through each route and add to a general route map.
        HashMap<Route, List<Alert>> routeMap = new HashMap<>();
        for (Alert alert : alertList) {
            Route route = new Route(alert.route.routeId, alert.route.routeName);
            List<Alert> alertsForRoute = routeMap.containsKey(route) ? routeMap.get(route) : new ArrayList<>();

            alertsForRoute.add(alert);
            routeMap.put(route, alertsForRoute);
        }

        List<Route> agencyRoutesList = new ArrayList<>();
        for (Map.Entry<Route, List<Alert>> routePair : routeMap.entrySet()) {
            Route agencyRoute = routePair.getKey();
            agencyRoute.alerts = routePair.getValue();
            agencyRoutesList.add(agencyRoute);
        }

        Agency agency = new Agency();
        agency.agencyName = AGENCY_NAME;
        agency.agencyId = 1;
        agency.routes = agencyRoutesList;

        return agency;
    }

    /**
     * Holding object so that we can keep hold of the
     * array of alerts.
     */
    private class SeptaHashMap extends HashMap<String, List<Alert>>{
        private HashMap<String, String> routeIdNameMap = new HashMap<>();

        private void setRouteName(String routeId, String routeName) {
            routeIdNameMap.put(routeId, routeName);
        }

        public String getRouteName(String routeId) {
            if (routeIdNameMap.containsKey(routeId)) {
                return routeIdNameMap.get(routeId);
            } else {
                return null;
            }
        }

        public List<Alert> put(String routeId, String routeName, Alert alert) {
            setRouteName(routeId, routeName);

            if (containsKey(routeId)) {
                get(routeId).add(alert);
                return get(routeId);

            } else {
                List<Alert> routeAlertList = new ArrayList<>();
                routeAlertList.add(alert);
                return put(routeId, routeAlertList);
            }
        }
    }
}
