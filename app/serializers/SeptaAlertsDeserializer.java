package serializers;

import com.google.gson.*;
import enums.AlertLevel;
import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import main.Log;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
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

    private static final String AGENCY_NAME = "South-East Pennsylvania Transit Association";
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

        // Map of route alerts.
        HashMap<Route, List<Alert>> routeAlertsMap = new HashMap<>();

        // Create agency and add the mapped routes.
        Agency agency = new Agency();
        agency.id = 1;
        agency.name = AGENCY_NAME;
        agency.phone = "12155807800";
        agency.externalUri = "http://www.septa.org";
        agency.utcOffset = -5f;

        final JsonArray schedulesArray = json.getAsJsonArray();
        if (schedulesArray != null) {

            try {
                // Loop through each alert row.
                for (JsonElement scheduleRow : schedulesArray) {
                    JsonObject bucket = scheduleRow.getAsJsonObject();

                    String routeId = bucket.get("route_id").getAsString();
                    String routeName = bucket.get("route_name").getAsString();
                    String advisoryMessage = bucket.get("advisory_message").getAsString();
                    String currentMessage = bucket.get("current_message").getAsString();
                    String detourStartDate = bucket.get("detour_start_date_time").getAsString();
                    String detourEndDate = bucket.get("detour_end_date_time").getAsString();
                    String detourMessage = bucket.get("detour_message").getAsString();
                    String detourReason = bucket.get("detour_reason").getAsString();
                    String detourStartLocation = bucket.get("detour_start_location").getAsString();
                    String isSnow = bucket.get("isSnow").getAsString();
                    String lastUpdated = bucket.get("last_updated").getAsString();

                    Calendar lastUpdateCalendar = Calendar.getInstance(timezone, Locale.US);
                    if (lastUpdated != null && !lastUpdated.isEmpty()) {
                        lastUpdateCalendar.setTime(lastUpdatedDateFormat.parse(lastUpdated));
                    }

                    // Instantiate the alert.
                    Alert alert = new Alert();
                    alert.lastUpdated = lastUpdateCalendar;

                    if (routeId != null) {
                        routeId = routeId.toLowerCase();

                        // Parse the detour locations into the correct type.
                        if (!detourMessage.isEmpty()) {

                            Calendar detourStartCalendar = Calendar.getInstance(timezone, Locale.US);
                            if (detourStartDate != null && !detourStartDate.isEmpty()) {
                                detourStartCalendar.setTime(detourDateFormat.parse(detourStartDate));
                            }

                            Calendar detourEndCalendar = Calendar.getInstance(timezone, Locale.US);
                            if (detourEndDate != null && !detourEndDate.isEmpty()) {
                                detourEndCalendar.setTime(detourDateFormat.parse(detourEndDate));
                            }

                            Location startLocation = new Location();
                            startLocation.name = detourStartLocation;
                            startLocation.date = detourStartCalendar;
                            startLocation.sequence = 0;
                            startLocation.message = detourReason;
                            List<Location> detourLocations = new ArrayList<>();
                            detourLocations.add(startLocation);

                            AlertType typeDetour = AlertType.TYPE_DETOUR;
                            alert.level = AlertLevel.LEVEL_LOW;
                            alert.type = typeDetour;
                            alert.messageTitle = typeDetour.title;
                            alert.messageSubtitle = detourReason;
                            alert.messageBody = detourMessage;
                            alert.locations = detourLocations;
                        }

                        // Snow Alerts
                        else if (isSnow.toLowerCase().equals("y")) {
                            AlertType typeWeather = AlertType.TYPE_WEATHER;

                            alert.level = AlertLevel.LEVEL_MEDIUM;
                            alert.type = typeWeather;
                            alert.messageTitle = typeWeather.title;
                            alert.messageBody = currentMessage;
                        }

                        // Advisory Alerts
                        else if (!advisoryMessage.isEmpty()) {
                            AlertType typeInformation = AlertType.TYPE_INFORMATION;

                            alert.level = AlertLevel.LEVEL_LOW;
                            alert.type = typeInformation;
                            alert.messageTitle = typeInformation.title;
                            alert.messageBody = advisoryMessage;
                        }

                        // Current Alerts
                        else if (!currentMessage.isEmpty()) {
                            AlertType typeCurrent = AlertType.TYPE_DISRUPTION;

                            alert.level = AlertLevel.LEVEL_MEDIUM;
                            alert.type = typeCurrent;
                            alert.messageTitle = typeCurrent.title;
                            alert.messageBody = currentMessage;
                        }

                        // One Route per multiple alerts, so get or add a route to a map.
                        Route route = new Route(routeId, routeName);
                        List<Alert> alerts = new ArrayList<>();

                        if (routeAlertsMap.containsKey(route)) {
                            alerts = routeAlertsMap.get(route);
                        }

                        // Set route transit types.
                        if (routeId.contains("generic")) {
                            route.transitType = TransitType.TYPE_SPECIAL;
                            route.externalUri = "http://www.septa.org/service/";

                        } else if (routeId.contains("cct")) {
                            route.transitType = TransitType.TYPE_SPECIAL;
                            route.externalUri = "http://www.septa.org/service/cct/";

                        } else if (routeId.contains("bsl")) {
                            route.transitType = TransitType.TYPE_SUBWAY;
                            route.externalUri = "http://www.septa.org/service/bsl/";

                        } else if (routeId.contains("mfl")) {
                            route.transitType = TransitType.TYPE_SUBWAY;
                            route.externalUri = "http://www.septa.org/service/mfl/";

                        } else if (routeId.contains("nhsl")) {
                            route.transitType = TransitType.TYPE_LIGHT_RAIL;
                            route.externalUri = "http://www.septa.org/service/highspeed/";

                        } else if (routeId.contains("bus_")) {
                            route.transitType = TransitType.TYPE_BUS;
                            route.externalUri = "http://www.septa.org/service/bus/";

                        } else if (routeId.contains("trolley_")) {
                            route.transitType = TransitType.TYPE_LIGHT_RAIL;
                            route.externalUri = "http://www.septa.org/service/trolley/";

                        } else if (routeId.contains("rr_")) {
                            route.transitType = TransitType.TYPE_RAIL;
                            route.externalUri = "http://www.septa.org/service/rail/";
                        }

                        // Set route flags.
                        if (routeId.contains("generic")) {
                            route.isSticky = true;
                            route.isDefault = true;

                        } else if (routeId.contains("bso") || routeId.contains("mfo")) {
                            route.routeFlag = RouteFlag.TYPE_OWL;
                        }

                        route.alerts.add(alert);
                        route.isDefault = false;
                        routeAlertsMap.put(route, alerts);
                    }

                    agency.routes = new ArrayList<>();

                    // Add each route and alert set to the agency routes.
                    for (Map.Entry<Route, List<Alert>> routeEntry : routeAlertsMap.entrySet()) {
                        Route route = routeEntry.getKey();

                        List<Alert> alerts = routeEntry.getValue();
                        route.alerts.addAll(alerts);
                        agency.routes.add(route);
                    }
                }

            } catch (IllegalStateException pe) {
                mLog.c(TAG, "Error parsing json body into alert object", pe);

            } catch (ParseException e) {
                mLog.c(TAG, "Error parsing json date(s) into alert object", e);
            }
        }

        mLog.d(TAG, "Finished creating SEPTA route-alert map.");
        return agency;
    }
}
