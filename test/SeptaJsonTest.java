import agency.SeptaAgencyUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enums.AlertType;
import enums.TransitType;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.DefaultApplication;
import play.Mode;
import play.api.inject.Binding;
import play.api.inject.BindingKey;
import play.api.routing.Router;
import play.inject.guice.GuiceApplicationBuilder;
import serializers.SeptaAlertsDeserializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

/**
 * SEPTA Agency test.
 */
public class SeptaJsonTest {
    private static final String JSON_FILE_NAME = "/resources/json_septa.json";
    private static Agency septaAgency;

    @BeforeClass
    public static void setup() throws IOException {
        Binding<Router> routesBindingOverride = new BindingKey<>(Router.class)
                .toProvider(CommuteTestApplication.MockRouterProvider.class)
                .eagerly();

        DefaultApplication application = (DefaultApplication) new GuiceApplicationBuilder()
                .in(Mode.TEST)
                .overrides(routesBindingOverride)
                .build();

        Path path = Paths.get(application.path().getCanonicalPath() + JSON_FILE_NAME);
        String jsonString = new String(Files.readAllBytes(path));

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Agency.class, new SeptaAlertsDeserializer(null))
                .create();

        septaAgency = gson.fromJson(jsonString, Agency.class);
    }

    @AfterClass
    public static void teardown() {

    }

    @Test
    public void testDeserializeAgency() {
        assertNotNull(septaAgency);
        assertEquals(septaAgency.name, SeptaAgencyUpdate.AGENCY_NAME);
        assertEquals(septaAgency.id, Integer.valueOf(SeptaAgencyUpdate.AGENCY_ID));
        assertNotNull(septaAgency.utcOffset);
        assertEquals(septaAgency.utcOffset, Float.valueOf(-5F));
    }

    @Test
    public void testDeserializeRoutes() {
        assertNotNull(septaAgency.routes);
        assertFalse(septaAgency.routes.isEmpty());
    }

    @Test
    public void testDeserializeBusAlerts() {
        Route busRoute80 = null;
        for (Route route : septaAgency.routes) {
            if (route.routeId.equals("bus_route_80")) {
                busRoute80 = route;
            }
        }

        String detour1Message = "SB Via Broad,<br>R - 71st Ave.,<br>L  - 15th St.,<br>L  - 68th Ave.,<br>R - Broad, <br>Reg  Rt";
        String detour1StartLocation = "71st & Broad";
        String detour1Reason = "Contractors";

        Calendar detour1StartDate = SeptaAlertsDeserializer.getParsedDate("8/22/2017   6:23 AM", true);
        Calendar detour1EndDate = SeptaAlertsDeserializer.getParsedDate("10/16/2017   4:30 AM", true);
        Calendar detour1LastUpdated = SeptaAlertsDeserializer.getParsedDate("Mar 15 2017 08:08:02:357AM", false);

        String detour2Message = "NB via Easton<br>L - Limekiln<br>R - Glenside<br>L - Easton<br>L - Mt. Carmel<br>Reg Rt";
        String detour2StartLocation = "Easton & Limekiln";
        String detour2Reason = "Weight Restriction";

        Calendar detour2StartDate = SeptaAlertsDeserializer.getParsedDate("1/3/2017   5:43 AM", true);
        Calendar detour2EndDate = SeptaAlertsDeserializer.getParsedDate("12/31/2017  11:59 PM", true);
        Calendar detour2LastUpdated = SeptaAlertsDeserializer.getParsedDate("Mar 15 2017 08:08:02:357AM", false);

        String detour3Message = "SB via Mt. Carmel<br>R - Easton<br>R - Glenside<br>L  - Limekiln<br>Reg Rt";
        String detour3StartLocation = "Easton & Limekiln";
        String detour3Reason = "Weight Restriction";

        Calendar detour3StartDate = SeptaAlertsDeserializer.getParsedDate("1/3/2017   5:44 AM", true);
        Calendar detour3EndDate = SeptaAlertsDeserializer.getParsedDate("12/31/2017  11:59 PM", true);
        Calendar detour3LastUpdated = SeptaAlertsDeserializer.getParsedDate("Mar 15 2017 08:08:02:357AM", false);

        String detour4Message = "NB Via Broad Street<br>R- Old York<br>L- Cheltenham <br>Reg Rt";
        String detour4StartLocation = "Broad & Old York";
        String detour4Reason = "Contractors";

        Calendar detour4StartDate = SeptaAlertsDeserializer.getParsedDate("8/22/2017   6:21 AM", true);
        Calendar detour4EndDate = SeptaAlertsDeserializer.getParsedDate("10/16/2017   4:30 AM", true);
        Calendar detour4LastUpdated = SeptaAlertsDeserializer.getParsedDate("Mar 15 2017 08:08:02:357AM", false);

        assertNotNull(busRoute80);
        assertNotNull(busRoute80.alerts);
        assertFalse(busRoute80.alerts.isEmpty());
        assertEquals(busRoute80.alerts.size(), 4);

        for (Alert alert : busRoute80.alerts) {
            assertNotNull(alert.route);

            assertNotNull(alert.type);
            assertTrue(alert.type == AlertType.TYPE_DETOUR);
            assertNotNull(alert.messageBody);
            assertNotNull(alert.messageTitle);
            assertNotNull(alert.highPriority);
            assertTrue(alert.highPriority);

            assertNotNull(alert.locations);
            assertEquals(alert.locations.size(), 2); // Start and end location.

            assertNotNull(alert.locations.get(0));
            assertNotNull(alert.locations.get(1));

            if (alert.messageBody.equals(detour1Message)) {
                assertEquals(alert.lastUpdated, detour1LastUpdated);

                Location startLocation = alert.locations.get(0);
                assertEquals(startLocation.name, detour1StartLocation);
                assertEquals(startLocation.message, detour1Reason);
                assertEquals(startLocation.sequence, Integer.valueOf(0));
                assertEquals(startLocation.date, detour1StartDate);
                assertNotNull(startLocation.alert);

                Location endLocation = alert.locations.get(1);
                assertEquals(endLocation.name, detour1StartLocation);
                assertEquals(endLocation.message, detour1Reason);
                assertEquals(endLocation.sequence, Integer.valueOf(-1));
                assertEquals(endLocation.date, detour1EndDate);
                assertNotNull(endLocation.alert);
                assertNotNull(endLocation.alert);

            } else if (alert.messageBody.equals(detour2Message)) {
                assertEquals(alert.lastUpdated, detour2LastUpdated);

                Location startLocation = alert.locations.get(0);
                assertEquals(startLocation.name, detour2StartLocation);
                assertEquals(startLocation.message, detour2Reason);
                assertEquals(startLocation.sequence, Integer.valueOf(0));
                assertEquals(startLocation.date, detour2StartDate);
                assertNotNull(startLocation.alert);

                Location endLocation = alert.locations.get(1);
                assertEquals(endLocation.name, detour2StartLocation);
                assertEquals(endLocation.message, detour2Reason);
                assertEquals(endLocation.sequence, Integer.valueOf(-1));
                assertEquals(endLocation.date, detour2EndDate);
                assertNotNull(endLocation.alert);

            } else if (alert.messageBody.equals(detour3Message)) {
                assertEquals(alert.lastUpdated, detour3LastUpdated);

                Location startLocation = alert.locations.get(0);
                assertEquals(startLocation.name, detour3StartLocation);
                assertEquals(startLocation.message, detour3Reason);
                assertEquals(startLocation.sequence, Integer.valueOf(0));
                assertEquals(startLocation.date, detour3StartDate);
                assertNotNull(startLocation.alert);

                Location endLocation = alert.locations.get(1);
                assertEquals(endLocation.name, detour3StartLocation);
                assertEquals(endLocation.message, detour3Reason);
                assertEquals(endLocation.sequence, Integer.valueOf(-1));
                assertEquals(endLocation.date, detour3EndDate);
                assertNotNull(endLocation.alert);

            } else if (alert.messageBody.equals(detour4Message)) {
                assertEquals(alert.lastUpdated, detour4LastUpdated);

                Location startLocation = alert.locations.get(0);
                assertEquals(startLocation.name, detour4StartLocation);
                assertEquals(startLocation.message, detour4Reason);
                assertEquals(startLocation.sequence, Integer.valueOf(0));
                assertEquals(startLocation.date, detour4StartDate);
                assertNotNull(startLocation.alert);

                Location endLocation = alert.locations.get(1);
                assertEquals(endLocation.name, detour4StartLocation);
                assertEquals(endLocation.message, detour4Reason);
                assertEquals(endLocation.sequence, Integer.valueOf(-1));
                assertEquals(endLocation.date, detour4EndDate);
                assertNotNull(endLocation.alert);
            }
        }
    }

    @Test
    public void testDeserializeTrolleyAlerts() {
        Route trolleyRoute13 = null;
        for (Route route : septaAgency.routes) {
            if (route.routeId.equals("trolley_route_13")) {
                trolleyRoute13 = route;
            }
        }

        assertNotNull(trolleyRoute13);
        assertNotNull(trolleyRoute13.transitType);
        assertEquals(trolleyRoute13.transitType, TransitType.TYPE_LIGHT_RAIL);

        assertNotNull(trolleyRoute13.alerts);
        assertTrue(trolleyRoute13.alerts.isEmpty());
    }

    @Test
    public void testDeserializeRailAlerts() {
        Route railRouteAir = null;
        for (Route route : septaAgency.routes) {
            if (route.routeId.equals("rr_route_apt")) {
                railRouteAir = route;
            }
        }

        Calendar lastUpdated = SeptaAlertsDeserializer.getParsedDate("Aug 25 2017 09:46:50:940PM", false);
        String alertMessage = "<div class=\"fares_container\">\n" +
                "\t\t\t\t <h3 class=\"separated\">Late Night Changes to Service</h3>\n" +
                "\t\t\t\t <p class=\"desc separated\">Beginning Monday, June 19, 2017</p>\n" +
                "\t\t\t\t <p>Due to an ongoing track improvement project, the following adjustments to late night service will take effect starting Monday, June 19th. This construction is expected to last 18 weeks.</p>\n" +
                "<ul>\n" +
                "<li><em><strong>Weekday </strong></em>service will terminate at Temple University Station for trains #466 (departing terminals at 9:07 p.m. - Alternate Option: Lansdale/Doylestown #594, departing Suburban at 9:50 p.m., with stops at Melrose, Elkins Park Stations), #470 (departing terminals at 10:07 p.m. - Alternate Option, Warminster #472, departing Suburban at 11:05 p.m.), and #474 (departing terminals at 11:07 p.m. - Alternate Option: Lansdale/Doylestown #596, departing Suburban at 11:35 p.m., with stops at Elkins, Melrose Park Stations)</li>\n" +
                "<br /> \n" +
                "</ul>\n" +
                "<ul>\n" +
                "<li><em><strong>Weekday </strong></em>Train #475 (departing Glenside at 10:23 p.m.) will be <em><strong>CANCELED</strong></em>, however, <strong>this train will still operate from Temple University</strong> (departing at 10:44 p.m.) with service to airport terminals. As an alternate for service from Glenside, customers should take Train #473 (departing Glenside at 9:41 p.m.) or Train #477 (departing Glenside at 10:51 p.m.)</li>\n" +
                "<br /> \n" +
                "</ul>\n" +
                "<ul>\n" +
                "<li><strong>Saturday &amp; Sunday</strong> Train #476 (departing terminals at 11:37 p.m.) service will terminate at Temple University Station (Alternate Option: Glenside #478, departing Suburban at 12:35 a.m.)</li>\n" +
                "</ul>\n" +
                "<p><strong>Opposite Side Boarding at Melrose &amp; Elkins Park Stations: 9:30 p.m. - End of Service</strong></p>\n" +
                "<p>While this work takes place, ALL trains will board from the <strong>INBOUND </strong>(toward Center City) platform at Melrose and Elkins Park Stations. Please listen for station announcements for any additional changes to service or boarding locations.</p>\n" +
                "<p><strong>For a complete list of the adjustments to service, please visit the <a title=\"www.septa.org/alert/construction/late-night.html\" href=\"/alert/construction/late-night.html\" target=\"_blank\">late night service page</a>.</strong></p>\n" +
                "\t\t\t </div>";


        assertNotNull(railRouteAir);

        assertNotNull(railRouteAir.transitType);
        assertEquals(railRouteAir.transitType, TransitType.TYPE_RAIL);
        assertNotNull(railRouteAir.routeName);
        assertEquals(railRouteAir.routeName, "Airport");

        assertNotNull(railRouteAir.alerts);
        assertFalse(railRouteAir.alerts.isEmpty());
        assertEquals(railRouteAir.alerts.size(), 1);

        assertEquals(railRouteAir.alerts.get(0).messageBody, alertMessage);
        assertEquals(railRouteAir.alerts.get(0).type, AlertType.TYPE_INFORMATION);
        assertEquals(railRouteAir.alerts.get(0).lastUpdated, lastUpdated);
    }

    @Test
    public void testDeserializeSubwayAlerts() {
        Route subwayMfl = null;
        Route subwayBsl = null;
        for (Route route : septaAgency.routes) {
            if (route.routeId.equals("rr_route_mfl")) {
                subwayMfl = route;
            } else if (route.routeId.equals("rr_route_bsl")) {
                subwayBsl = route;
            }
        }

        String mflAlertMessage = "<div class=\"fares_container\">\n" +
                "\t\t\t\t <h3 class=\"separated\">Temporary Opposite Side Boarding | 15th, 13th, 11th, and 8th St. Stations</h3>\n" +
                "\t\t\t\t <p class=\"desc separated\">Dates Noted Below</p>\n" +
                "\t\t\t\t <p>Due to the improvement project at 15th St. Station, ALL trains will board from the <strong>opposite side </strong>platform at <strong>15th, 13th, 11th, and 8th St. Stations</strong> as noted below:</p>\n" +
                "<p style=\"text-align: center;\"><span style=\"font-size: medium;\">All trains board from the <strong>EASTBOUND </strong>(toward Frankford <br />Transportation Center) platform during the dates noted below:</span></p>\n" +
                "<p><strong>Saturday, August 5 at 4:30 a.m. and continuing through the end of service, Sunday, August 6, 2017</strong></p>\n" +
                "<p style=\"text-align: center;\"><span style=\"font-size: medium;\">All trains board from the <strong>WESTBOUND </strong>(toward <br />69th St. Transportation Center) platform during the dates noted below:</span></p>\n" +
                "<p><strong>Saturdays at 4:30 a.m. and continuing through the End of Service Sundays, August 12-13, 19-20 &amp; 26-27, 2017</strong></p>\n" +
                "<p><strong>Boarding locations are subject to change. Please listen for station announcements for any additional changes to boarding locations.</strong></p>\n" +
                "\t\t\t </div>\n" +
                "\t\t\t <div class=\"fares_container\">\n" +
                "\t\t\t\t <h3 class=\"separated\"><a name=\"arrott\"></a><b>Arrott Transportation Center:</b> Temporary Closure of Stairs & Station Escalator, Relocation of Fare Line & Opposite Side Boarding</h3>\n" +
                "\t\t\t\t <p class=\"desc separated\">Service changes & dates noted below:</p>\n" +
                "\t\t\t\t <p>Due to the <a title=\"www.septa.org/rebuilding/station/margaret.html\" href=\"/rebuilding/station/margaret.html\" target=\"_blank\">improvement project at Arrott Transportation Center</a>, the following impacts to the station will take effect starting Monday, July 31st:</p>\n" +
                "<p><strong>STAIRWAY &amp; ESCALATOR CLOSURE: </strong>Customers should use the temporary staircase located across Arrott St.</p>\n" +
                "<p><strong>RELOCATION OF FARE LINE:</strong> Customers should use the temporary fare line located on the Westbound side of the station, south of the existing stairway at street level.</p>\n" +
                "<p><strong>Alternate accessibility options during construction:</strong></p>\n" +
                "<p>As an alternate to the temporary stairway, customers should take Bus Routes 3 or 5 for service to Frankford Transportation Center or Church Station.</p>\n" +
                "<p>Please continue to check System Status for updates or changes to service.</p>\n" +
                "<p><span style=\"font-size: small;\"><strong>Opposite Side Boarding</strong></span></p>\n" +
                "<p><strong>Nightly, Monday - Friday, August 14-18, 21-25 &amp; 28- Sep. 1, 2017</strong></p>\n" +
                "<p>ALL trains will board from the <strong>EASTBOUND </strong>(toward Frankford Transportation Center) platform at Arrott Transportation Center and Church Station beginning at 8:15 p.m. and continuing until the end of the service day.</p>\n" +
                "\t\t\t </div>";

        Calendar mflLastUpdated = SeptaAlertsDeserializer.getParsedDate("Aug 25 2017 01:30:02:000PM", false);

        assertNotNull(subwayMfl);

        assertNotNull(subwayMfl.transitType);
        assertEquals(subwayMfl.transitType, TransitType.TYPE_SUBWAY);
        assertNotNull(subwayMfl.routeName);
        assertEquals(subwayMfl.routeName, "Market/Frankford Line");

        assertNotNull(subwayMfl.alerts);
        assertFalse(subwayMfl.alerts.isEmpty());
        assertEquals(subwayMfl.alerts.size(), 1);

        assertEquals(mflAlertMessage, subwayMfl.alerts.get(0).messageBody);
        assertEquals(AlertType.TYPE_INFORMATION, subwayMfl.alerts.get(0).type);
        assertEquals(mflLastUpdated, subwayMfl.alerts.get(0).lastUpdated);

        assertNotNull(subwayBsl.transitType);
        assertEquals(subwayBsl.transitType, TransitType.TYPE_SUBWAY);
        assertNotNull(subwayBsl.routeName);
        assertEquals(subwayBsl.routeName, "Broad Street Line");

        assertNotNull(subwayBsl);
        assertNotNull(subwayBsl.alerts);
        assertTrue(subwayBsl.alerts.isEmpty());
    }
}