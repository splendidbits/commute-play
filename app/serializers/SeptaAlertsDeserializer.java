package serializers;

import com.google.gson.*;
import enums.AlertLevel;
import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import services.splendidlog.Log;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    private static final TimeZone timezone = TimeZone.getTimeZone("UTC");

    private Log mLog;
    private Agency mAgency;

    public SeptaAlertsDeserializer(@Nonnull Log log, @Nullable Agency partialAgency) {
        mLog = log;
        mAgency = partialAgency;

        // Create agency if there was no partially filled agency from the client.
        if (mAgency == null) {
            mAgency = new Agency();
            mAgency.id = 1;
            mAgency.name = "South East Pennsylvania Transit Association";
            mAgency.phone = "12155807800";
            mAgency.externalUri = "http://www.septa.org";
            mAgency.utcOffset = -5f;
            mAgency.routes = new ArrayList<>();
        }
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

        // Map of route objects containing alerts. // [routeId, Route]
        HashMap<String, Route> routesMap = new HashMap<>();

        try {
            final JsonArray schedulesArray = json.getAsJsonArray();
            if (schedulesArray != null) {


                /*
                 * Loop through each alert row and separate each one into multiple possible alerts. This is
                 * because SEPTA overload each "alert" row with possibly more than one alert type of alert
                 * (advisory, current message, and detour.
                 *
                 * After all possible alerts in each row have been parsed for all rows, then loop through them
                 * and add them to the correct route object.
                 */
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

                    if (routeId != null) {
                        routeId = routeId.toLowerCase();

                        // Keep hold of all the possible alerts FOR THIS ROW.
                        List<Alert> rowAlerts = new ArrayList<>();

                        Calendar lastUpdateCalendar = Calendar.getInstance(timezone, Locale.US);
                        if (lastUpdated != null && !lastUpdated.isEmpty()) {
                            lastUpdateCalendar.setTime(lastUpdatedDateFormat.parse(lastUpdated));
                        }

                        // Parse the detour locations into the correct type.
                        if (!detourMessage.isEmpty()) {

                            Calendar detourStartCalendar = Calendar.getInstance(timezone, Locale.US);
                            Calendar detourEndCalendar = Calendar.getInstance(timezone, Locale.US);

                            // Add the detour start and end locations if they exist.
                            ArrayList<Location> detourLocations = new ArrayList<>();
                            if (detourStartDate != null && !detourStartDate.isEmpty()) {
                                detourStartCalendar.setTime(detourDateFormat.parse(detourStartDate));

                                Location detourLocation = new Location();
                                detourLocation.name = detourStartLocation;
                                detourLocation.date = detourStartCalendar;
                                detourLocation.sequence = 0;
                                detourLocation.message = detourReason;
                                detourLocations.add(detourLocation);
                            }

                            if (detourEndDate != null && !detourEndDate.isEmpty()) {
                                detourEndCalendar.setTime(detourDateFormat.parse(detourEndDate));

                                Location detourLocation = new Location();
                                detourLocation.date = detourEndCalendar;
                                detourLocation.sequence = -1;
                                detourLocation.message = detourReason;
                                detourLocations.add(detourLocation);
                            }

                            AlertType type = AlertType.TYPE_DETOUR;
                            Alert alert = new Alert();
                            alert.lastUpdated = lastUpdateCalendar;
                            alert.level = AlertLevel.LEVEL_LOW;
                            alert.type = type;
                            alert.messageTitle = type.title;
                            alert.messageSubtitle = detourReason;
                            alert.messageBody = detourMessage;
                            alert.locations = detourLocations;
                            rowAlerts.add(alert);
                        }

                        // Snow Alerts
                        if (isSnow.toLowerCase().equals("y")) {

                            AlertType typeWeather = AlertType.TYPE_WEATHER;
                            Alert alert = new Alert();
                            alert.lastUpdated = lastUpdateCalendar;
                            alert.level = AlertLevel.LEVEL_NORMAL;
                            alert.type = typeWeather;
                            alert.messageTitle = typeWeather.title;
                            alert.messageBody = currentMessage;
                            rowAlerts.add(alert);
                        }

                        // Advisory Alerts
                        if (!advisoryMessage.isEmpty()) {

                            AlertType typeInformation = AlertType.TYPE_INFORMATION;
                            Alert alert = new Alert();
                            alert.lastUpdated = lastUpdateCalendar;
                            alert.level = AlertLevel.LEVEL_LOW;
                            alert.type = typeInformation;
                            alert.messageTitle = typeInformation.title;
                            alert.messageBody = advisoryMessage;
                            rowAlerts.add(alert);
                        }

                        // Current Alerts
                        if (!currentMessage.isEmpty()) {

                            AlertType typeCurrent = AlertType.TYPE_DISRUPTION;
                            Alert alert = new Alert();
                            alert.lastUpdated = lastUpdateCalendar;
                            alert.level = AlertLevel.LEVEL_NORMAL;
                            alert.type = typeCurrent;
                            alert.messageTitle = typeCurrent.title;
                            alert.messageBody = currentMessage;
                            rowAlerts.add(alert);
                        }

                        /*
                         * There's now a list of (possibly empty) alerts for this single array entry in the
                         * json document. Check to see if there's already a Route model stored for this routeId.
                         *
                         * If a Route object doesn't exist; create it. If one exists; fetch it. Then add all the
                         * above alerts to the Route, and progress to the next alert row of the document.
                         */
                        Route route = new Route(routeId, routeName);
                        if (routesMap.containsKey(route.routeId)) {
                            route = routesMap.get(route.routeId);
                        }

                        // Create the Route model, as it may be the first time this route has been seen.
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

                        // Add alerts to route.
                        if (!rowAlerts.isEmpty()) {
                            route.alerts.addAll(rowAlerts);
                        }

                        // Add the modified route back into the map.
                        routesMap.put(routeId, route);
                    }
                }

                // Add all routes to the agency.
                mAgency.routes.addAll(routesMap.values());
            }

        } catch (IllegalStateException pe) {
            mLog.c(TAG, "Error parsing json body into alert object", pe);

        } catch (ParseException e) {
            mLog.c(TAG, "Error parsing json date(s) into alert object", e);
        }

        Collections.sort(mAgency.routes);
        mLog.d(TAG, "Finished creating and sorting SEPTA route-alert map.");

        return mAgency;
    }
}
