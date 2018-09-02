package serializers;

import agency.SeptaAgencyUpdate;
import com.google.gson.*;
import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import helpers.AlertHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import play.Logger;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Gson SEPTA Alerts Deserializer. Convert the SEPTA www3 alerts feed into
 * the commute GCM agency alerts bundle models.
 */
public class SeptaAlertsDeserializer implements JsonDeserializer<Agency> {
    private static final String[] IGNORED_ROUTES = {"bus_app"};
    private static final TimeZone timezone = TimeZone.getTimeZone("EST");

    @Override
    public Agency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Logger.info("Started parsing SEPTA alerts json body");

        Agency agency = new Agency(SeptaAgencyUpdate.AGENCY_ID);
        agency.setName(SeptaAgencyUpdate.AGENCY_NAME);
        agency.setPhone("+1 215 580 7800");
        agency.setExternalUri("http://www.septa.org");
        agency.setUtcOffset(-5f);

        // Map of route objects containing alerts. // [routeId, Route]
        HashMap<String, Route> routesMap = new HashMap<>();

        try {
            final JsonArray schedulesArray = json.getAsJsonArray();
            if (schedulesArray != null) {

                // Iterate through each object in the alerts document.
                for (JsonElement scheduleRow : schedulesArray) {
                    JsonObject bucket = scheduleRow.getAsJsonObject();

                    String routeId = !bucket.get("route_id").isJsonNull()
                            ? bucket.get("route_id").getAsString()
                            : "";

                    String routeName = !bucket.get("route_name").isJsonNull()
                            ? bucket.get("route_name").getAsString()
                            : "";

                    String advisoryMessage = !bucket.get("advisory_message").isJsonNull()
                            ? bucket.get("advisory_message").getAsString()
                            : "";

                    String currentMessage = !bucket.get("current_message").isJsonNull()
                            ? bucket.get("current_message").getAsString()
                            : "";

                    String detourStartDate = !bucket.get("detour_start_date_time").isJsonNull()
                            ? bucket.get("detour_start_date_time").getAsString()
                            : "";

                    String detourEndDate = !bucket.get("detour_end_date_time").isJsonNull()
                            ? bucket.get("detour_end_date_time").getAsString()
                            : "";

                    String detourMessage = !bucket.get("detour_message").isJsonNull()
                            ? bucket.get("detour_message").getAsString()
                            : "";

                    String detourReason = !bucket.get("detour_reason").isJsonNull()
                            ? bucket.get("detour_reason").getAsString()
                            : "";

                    String detourStartLocation = !bucket.get("detour_start_location").isJsonNull()
                            ? bucket.get("detour_start_location").getAsString()
                            : "";

                    String isSnow = !bucket.get("isSnow").isJsonNull()
                            ? bucket.get("isSnow").getAsString()
                            : "";

                    String lastUpdated = !bucket.get("last_updated").isJsonNull()
                            ? bucket.get("last_updated").getAsString()
                            : "";

                    if (routeId != null) {
                        routeId = routeId.toLowerCase();

                        // SKIP SOME ROUTES
                        boolean ignoreRoute = false;
                        for (String route : IGNORED_ROUTES) {
                            if (route.toLowerCase().equals(routeId.toLowerCase())) {
                                Logger.info(String.format("Ignoring route %s for agency %s", route, agency.getName()));
                                ignoreRoute = true;
                                break;
                            }
                        }
                        if (ignoreRoute) {
                            continue;
                        }

                        /*
                         * Routes Parsing:
                         *
                         * If a Route object doesn't exist; create it. If one exists; fetch it. Then add all the
                         * above alerts to the Route, and progress to the next alert row of the document.
                         *
                         * There's should be a list of (possibly empty) alerts for this single array entry in the
                         * json document. Check to see if there's already a Route model stored for this routeId.
                         */
                        Route route = new Route(routeId, routeName);

                        if (routesMap.containsKey(route.getRouteId())) {
                            route = routesMap.get(route.getRouteId());
                        }

                        // Create the Route model, as it may be the first time this route has been seen.
                        if (routeId.contains("generic")) {
                            route.setTransitType(TransitType.TYPE_SPECIAL);
                            route.setExternalUri("http://www.septa.org/service/");
                            route.setRouteName("General");

                        } else if (routeId.contains("cct")) {
                            route.setTransitType(TransitType.TYPE_SPECIAL);
                            route.setExternalUri("http://www.septa.org/service/cct/");
                            route.setRouteName("CCT Connect");

                        } else if (routeId.contains("bsl")) {
                            route.setTransitType(TransitType.TYPE_SUBWAY);
                            route.setExternalUri("http://www.septa.org/service/bsl/");

                        } else if (routeId.contains("mfl")) {
                            route.setTransitType(TransitType.TYPE_SUBWAY);
                            route.setExternalUri("http://www.septa.org/service/mfl/");

                        } else if (routeId.contains("nhsl")) {
                            route.setTransitType(TransitType.TYPE_LIGHT_RAIL);
                            route.setExternalUri("http://www.septa.org/service/highspeed/");

                        } else if (routeId.contains("bus_")) {
                            route.setTransitType(TransitType.TYPE_BUS);
                            route.setExternalUri("http://www.septa.org/service/bus/");

                        } else if (routeId.contains("trolley_")) {
                            route.setTransitType(TransitType.TYPE_LIGHT_RAIL);
                            route.setExternalUri("http://www.septa.org/service/trolley/");

                        } else if (routeId.contains("rr_")) {
                            route.setTransitType(TransitType.TYPE_RAIL);
                            route.setExternalUri("http://www.septa.org/service/rail/");
                        }

                        // Set route flags.
                        if (routeId.contains("generic")) {
                            route.setSticky(true);
                            route.setDefault(true);

                        } else if (routeId.contains("bso") || routeId.contains("mfo")) {
                            route.setRouteFlag(RouteFlag.TYPE_OWL);
                        }

                        /*
                         * Alerts Parsing:
                         *
                         * Loop through each alert row and separate each one into multiple possible alerts. This is
                         * because SEPTA overload each "alert" row with possibly more than one alert type of alert
                         * (advisory, current message, and detour.
                         *
                         * After all possible alerts in each row have been parsed for all rows, then loop through them
                         * and add them to the correct route object.
                         */
                        List<Alert> rowAlerts = new ArrayList<>();

                        // Parse the detour locations into the correct type.
                        if (!detourMessage.isEmpty()) {
                            AlertType type = AlertType.TYPE_DETOUR;
                            Alert alert = new Alert();
                            alert.setHighPriority(true);
                            alert.setLastUpdated(getParsedDate(lastUpdated, false));
                            alert.setType(type);
                            alert.setMessageTitle(type.title);
                            alert.setMessageSubtitle(detourReason);
                            alert.setMessageBody(detourMessage);
                            alert.setId(AlertHelper.createHash(alert, routeId));

                            // Add the detour startup and end locations if they exist.
                            ArrayList<Location> detourLocations = new ArrayList<>();
                            if (!detourStartDate.isEmpty()) {
                                Location startLocation = new Location();
                                startLocation.setName(detourStartLocation);
                                startLocation.setDate(getParsedDate(detourStartDate, true));
                                startLocation.setSequence(0);
                                startLocation.setMessage(detourReason);
                                startLocation.setId(AlertHelper.createHash(startLocation, alert.getId()));

                                detourLocations.add(startLocation);
                            }

                            if (!detourEndDate.isEmpty()) {
                                Location endLocation = new Location();
                                endLocation.setName(detourStartLocation);
                                endLocation.setDate(getParsedDate(detourEndDate, true));
                                endLocation.setSequence(-1);
                                endLocation.setMessage(detourReason);
                                endLocation.setId(AlertHelper.createHash(endLocation, alert.getId()));

                                detourLocations.add(endLocation);
                            }

                            alert.setLocations(detourLocations);

                            rowAlerts.add(alert);
                        }

                        Calendar lastUpdateCalendar = getParsedDate(lastUpdated, false);

                        // Snow Alerts
                        if (isSnow.toLowerCase().equals("y")) {
                            AlertType typeWeather = AlertType.TYPE_WEATHER;
                            Alert alert = new Alert();
                            alert.setHighPriority(true);
                            alert.setLastUpdated(lastUpdateCalendar);
                            alert.setType(typeWeather);
                            alert.setMessageTitle(typeWeather.title);
                            alert.setMessageBody(currentMessage);
                            alert.setId(AlertHelper.createHash(alert, routeId));

                            rowAlerts.add(alert);
                        }

                        // Advisory Alerts
                        if (!advisoryMessage.isEmpty()) {
                            AlertType typeInformation = AlertType.TYPE_INFORMATION;
                            Alert alert = new Alert();
                            alert.setHighPriority(false);
                            alert.setLastUpdated(lastUpdateCalendar);
                            alert.setType(typeInformation);
                            alert.setMessageTitle(typeInformation.title);
                            alert.setMessageBody(advisoryMessage);
                            alert.setId(AlertHelper.createHash(alert, routeId));

                            rowAlerts.add(alert);
                        }

                        // Current Alerts
                        if (!currentMessage.isEmpty()) {
                            AlertType typeCurrent = AlertType.TYPE_DISRUPTION;
                            Alert alert = new Alert();
                            alert.setHighPriority(true);
                            alert.setLastUpdated(lastUpdateCalendar);
                            alert.setType(typeCurrent);
                            alert.setMessageTitle(typeCurrent.title);
                            alert.setMessageBody(currentMessage);
                            alert.setId(AlertHelper.createHash(alert, routeId));

                            rowAlerts.add(alert);
                        }

                        // Add alerts to route.
                        route.setAlerts(rowAlerts);

                        // Add the modified route back into the map.
                        routesMap.put(routeId, route);
                    }
                }

                // Add all routes to the agency.
                if (!routesMap.isEmpty()) {
                    List<Route> routes = new ArrayList<>(routesMap.values());
                    agency.setRoutes(routes);
                }
            }

        } catch (IllegalStateException pe) {
            Logger.error("Error parsing json body into alert object", pe);
        }

        Collections.sort(agency.getRoutes());
        Logger.info("Finished creating and sorting SEPTA route-alert map.");

        return agency;
    }

    public static Calendar getParsedDate(String jsonDate, boolean allowNullDate) {
        if (jsonDate != null && !jsonDate.isEmpty()) {

            SimpleDateFormat dateFormat1 = new SimpleDateFormat("MMM dd yyyy hh:mm:ss.SSSa", Locale.US);
            dateFormat1.setLenient(true);
            dateFormat1.setTimeZone(TimeZone.getTimeZone("EST"));

            // last_updated 2 - 2017-03-14 15:56:57.090
            SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            dateFormat2.setLenient(true);
            dateFormat2.setTimeZone(TimeZone.getTimeZone("EST"));

            // detour        - 07/14/2016   9:26 AM
            SimpleDateFormat dateFormat3 = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);
            dateFormat3.setLenient(true);
            dateFormat3.setTimeZone(TimeZone.getTimeZone("EST"));

            List<SimpleDateFormat> dateFormatters = Arrays.asList(dateFormat1, dateFormat2, dateFormat3);
            for (SimpleDateFormat formatter : dateFormatters) {
                try {
                    Calendar calendar = Calendar.getInstance(timezone, Locale.US);
                    calendar.setTime(formatter.parse(jsonDate));
                    return calendar;

                } catch (ParseException e) {
                }
            }
        }

        if (!allowNullDate) {
            Logger.warn(String.format("Error Parsing date %s. Using epoch start time time.", jsonDate));
            Calendar epochStart = Calendar.getInstance(timezone, Locale.US);
            epochStart.setTimeInMillis(0);
            return epochStart;
        }

        return null;
    }
}
