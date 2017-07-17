import dao.AgencyDao;
import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AlertsTest extends CommuteTestApplication {
    private static AgencyDao mAgencyDao;

    private final Calendar locationDate = Calendar.getInstance();
    private final static int AGENCY_ID = 999;
    private final static String ROUTE1_ID = "test_route_1";
    private final static String ROUTE2_ID = "test_route_2";

    private Agency initialAgency;
    private Route initialRoute;
    private Alert initialAlert;
    private Location initialLocation;

    @BeforeClass
    public static void initialise() {
        mAgencyDao = application.injector().instanceOf(AgencyDao.class);
    }

    @Before
    public void beforeTest() {
        initialLocation = new Location();
        initialLocation.latitude = "-75.1653533";
        initialLocation.longitude = "39.9517899";
        initialLocation.message = "Location Message";
        initialLocation.name = "Location Name";
        initialLocation.sequence = 100;
        initialLocation.date = locationDate;

        initialAlert = new Alert();
        initialAlert.highPriority = false;
        initialAlert.messageTitle = "Alert Message Title";
        initialAlert.messageSubtitle = "Alert Message Subtitle";
        initialAlert.messageBody = "Alert Message Body";
        initialAlert.type = AlertType.TYPE_INFORMATION;
        initialAlert.locations = Arrays.asList(initialLocation);
        initialAlert.lastUpdated = locationDate;

        initialRoute = new Route(ROUTE1_ID);
        initialRoute.externalUri = "http://example.com";
        initialRoute.isSticky = false;
        initialRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        initialRoute.transitType = TransitType.TYPE_SPECIAL;
        initialRoute.routeName = "Route Name";
        initialRoute.alerts = Arrays.asList(initialAlert);

        initialAgency = new Agency(AGENCY_ID);
        initialAgency.name = "Test Agency";
        initialAgency.externalUri = "http://example.com";
        initialAgency.phone = "123-123-1234";
        initialAgency.utcOffset = -5F;
        initialAgency.routes = Arrays.asList(initialRoute);
    }

    @After
    public void afterTest() {
        mAgencyDao.removeAgency(AGENCY_ID);
        assertNull(mAgencyDao.getAgency(AGENCY_ID));

        assertNotNull(mAgencyDao.getRoutes(AGENCY_ID));
        assertTrue(mAgencyDao.getRoutes(AGENCY_ID).isEmpty());
    }

    @Test
    public void testDatabaseAgencyInsert() {
        // Save the agency
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency, check everything is still there.
        Agency savedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(savedAgency);
        assertNotNull(savedAgency.routes);
        assertFalse(savedAgency.routes.isEmpty());

        for (Route savedRoute : savedAgency.routes) {
            assertNotNull(savedRoute.alerts);
            assertFalse(savedRoute.alerts.isEmpty());

            for (Alert savedAlert : savedRoute.alerts) {
                assertNotNull(savedAlert.locations);
                assertFalse(savedAlert.locations.isEmpty());
            }
        }
    }

    @Test
    public void testDatabaseAgencyUpdate() {
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency
        Agency savedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(savedAgency);

        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        updatedAgency.name = "Updated Name";
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertEquals(updatedAgency.name, "Updated Name");
    }

    @Test
    public void testDatabaseRouteAdd() {
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency
        Agency savedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(savedAgency);

        // Add the second route
        Route addedRoute = new Route(ROUTE2_ID);
        addedRoute.externalUri = "http://example.com";
        addedRoute.isSticky = false;
        addedRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        addedRoute.transitType = TransitType.TYPE_SPECIAL;
        addedRoute.routeName = "Route 2 Name";

        // Ensure the Agency was updated
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        updatedAgency.name = "Updated Name";

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Add the new route
        updatedAgency.routes.add(addedRoute);
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertEquals(updatedAgency.name, "Updated Name");
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 2);

        // Check it contains the previous route
        assertTrue(updatedAgency.routes.contains(addedRoute));
    }

    @Test
    public void testDatabaseAlertsAdd() {
        // Add the second route and save.
        Route addedRoute = new Route(ROUTE2_ID);
        addedRoute.externalUri = "http://example.com";
        addedRoute.isSticky = false;
        addedRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        addedRoute.transitType = TransitType.TYPE_SPECIAL;
        addedRoute.routeName = "Route 2 Name";

        List<Route> routes = new ArrayList<>();
        routes.add(initialRoute);
        routes.add(addedRoute);
        initialAgency.routes = routes;

        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Ensure the routes were added.
        assertNotNull(updatedAgency);
        assertNotNull(updatedAgency.routes);
        assertFalse(updatedAgency.routes.isEmpty());
        assertTrue(updatedAgency.routes.contains(addedRoute));
        assertEquals(updatedAgency.routes.size(), 2);

        // Update the route with the the alerts
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        Route routeToUpdate = null;
        for (Route route : updatedAgency.routes) {
            if (route.routeId.equals(ROUTE1_ID)) {
                routeToUpdate = route;
                break;
            }
        }

        assertNotNull(routeToUpdate);
        assertNotNull(routeToUpdate.alerts);
        assertFalse(routeToUpdate.alerts.isEmpty());
        assertEquals(routeToUpdate.alerts.size(), 1);

        // Modify route1 to have one new alert with no locations, and the existing alert
        Alert newAlert1 = new Alert();
        newAlert1.highPriority = false;
        newAlert1.messageTitle = "New Alert Message Title";
        newAlert1.messageSubtitle = "New Alert Message Subtitle";
        newAlert1.messageBody = "New Alert Message Body";
        newAlert1.type = AlertType.TYPE_DETOUR;
        newAlert1.lastUpdated = Calendar.getInstance();

        List<Alert> alertList = new ArrayList<>();

        initialAlert.locations = Arrays.asList(initialLocation);
        alertList.add(initialAlert);
        alertList.add(newAlert1);
        initialRoute.alerts = alertList;
        mAgencyDao.saveAgency(initialAgency);

        // Check the new alerts are there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 2);

        for (Route route : updatedAgency.routes) {

            if (route.routeId.equals(ROUTE1_ID)) {
                // Check that the initial alert still has the locations.
                assertNotNull(route.alerts);
                for (Alert alert : route.alerts) {
                    if (alert.messageTitle.equals("Alert Message Title")) {
                        assertNotNull(alert.locations);
                        assertFalse(alert.locations.isEmpty());
                    }
                }

                assertTrue(route.alerts.contains(newAlert1));
                assertTrue(route.alerts.contains(initialAlert));

            } else if (route.routeId.equals(ROUTE2_ID)) {
                assertNotNull(route.alerts);
                assertTrue(route.alerts.isEmpty());
            }
        }
    }

    // Route Remove

    // Alert Location Remove

    // Alert location Update

    // All route alerts remove

    // Alert Modifications add updated route
    // Alert Modifications add updated alert
    // Alert Modifications add stale route
    // Alert Modifications add stale alert

}
