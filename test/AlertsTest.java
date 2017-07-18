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

import java.util.*;

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
        initialAlert.locations = Collections.singletonList(initialLocation);
        initialAlert.lastUpdated = locationDate;

        initialRoute = new Route(ROUTE1_ID);
        initialRoute.externalUri = "http://example.com";
        initialRoute.isSticky = false;
        initialRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        initialRoute.transitType = TransitType.TYPE_SPECIAL;
        initialRoute.routeName = "Route Name";
        initialRoute.alerts = Collections.singletonList(initialAlert);

        initialAgency = new Agency(AGENCY_ID);
        initialAgency.name = "Test Agency";
        initialAgency.externalUri = "http://example.com";
        initialAgency.phone = "123-123-1234";
        initialAgency.utcOffset = -5F;
        initialAgency.routes = Collections.singletonList(initialRoute);
    }

    @After
    public void afterTest() {
        // Remove agency and test things are removed.
        mAgencyDao.removeAgency(AGENCY_ID);
    }

    @Test
    public void testDatabaseAgencyInsert() throws CloneNotSupportedException {
        // Save the agency
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency, check everything is still there.
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Check the agency has the Alert.
        assertNotNull(updatedAgency.routes.get(0));
        assertNotNull(updatedAgency.routes.get(0).alerts);
        assertEquals(updatedAgency.routes.get(0).alerts.size(), 1);

        // Check the agency has the Location.
        assertNotNull(updatedAgency.routes.get(0).alerts.get(0).locations);
        assertEquals(updatedAgency.routes.get(0).alerts.get(0).locations.size(), 1);
    }

    @Test
    public void testDatabaseAgencyUpdate() {
        mAgencyDao.saveAgency(initialAgency);

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
    public void testDatabaseRouteUpdate() {
        mAgencyDao.saveAgency(initialAgency);

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

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Add the new route
        updatedAgency.routes.add(addedRoute);
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 2);

        // Check it contains the previous route
        assertTrue(updatedAgency.routes.contains(addedRoute));
    }

    @Test
    public void testDatabaseAlertsAdd() {
        mAgencyDao.saveAgency(initialAgency);

        // Modify route1 to have one new alert with no locations, and the existing alert
        List<Alert> alerts = new ArrayList<>();

        Alert newAlert1 = new Alert();
        newAlert1.highPriority = false;
        newAlert1.messageTitle = "New Alert Message Title";
        newAlert1.messageSubtitle = "New Alert Message Subtitle";
        newAlert1.messageBody = "New Alert Message Body";
        newAlert1.type = AlertType.TYPE_DETOUR;
        newAlert1.lastUpdated = Calendar.getInstance();

        alerts.add(initialAlert);
        alerts.add(newAlert1);

        initialRoute.alerts = alerts;
        initialAgency.routes = Collections.singletonList(initialRoute);
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the routes are correct.
        assertNotNull(updatedAgency.routes);
        assertFalse(updatedAgency.routes.isEmpty());
        assertEquals(updatedAgency.routes.size(), 1);

        // Check that the alerts have persisted
        List<Alert> newAlerts = updatedAgency.routes.get(0).alerts;
        assertNotNull(newAlerts);
        assertEquals(newAlerts.size(), 2);

        // Check the agency has the Location.
        for (Alert alert : newAlerts) {
            if (alert.messageTitle.equals("Alert Message Title")) {
                assertNotNull(alert.locations);
                assertEquals(alert.locations.size(), 1);
            }
        }

        assertTrue(newAlerts.contains(newAlert1));
        assertTrue(newAlerts.contains(initialAlert));
    }

    @Test
    public void testDatabaseRouteRemove() {
        mAgencyDao.saveAgency(initialAgency);

        assertTrue(mAgencyDao.removeAgency(AGENCY_ID));
        assertNull(mAgencyDao.getAgency(AGENCY_ID));
        assertNotNull(mAgencyDao.getRoutes(AGENCY_ID));
        assertTrue(mAgencyDao.getRoutes(AGENCY_ID).isEmpty());
    }

    @Test
    public void testDatabaseAlertRemove() {
        initialRoute.alerts = null;
        initialAgency.routes = Collections.singletonList(initialRoute);

        mAgencyDao.saveAgency(initialAgency);
        List<Route> routes = mAgencyDao.getRoutes(AGENCY_ID);

        assertNotNull(routes);
        assertEquals(routes.size(), 1);
        assertTrue(routes.get(0).alerts == null || routes.get(0).alerts.isEmpty());
    }

    @Test
    public void tesDatabasetLocationRemove() {
        initialAlert.locations = null;
        mAgencyDao.saveAgency(initialAgency);
        List<Route> routes = mAgencyDao.getRoutes(AGENCY_ID);

        assertNotNull(routes);
        assertEquals(routes.size(), 1);

        assertNotNull(routes.get(0).alerts);
        assertEquals(routes.get(0).alerts.size(), 1);
        assertTrue(routes.get(0).alerts.get(0).locations == null || routes.get(0).alerts.get(0).locations.isEmpty());
    }

    @Test
    public void testLocationUpdate() {
        mAgencyDao.saveAgency(initialAgency);

        // Update the location
        initialLocation.message = "new message";
        initialAlert.locations = Collections.singletonList(initialLocation);
        initialRoute.alerts = Collections.singletonList(initialAlert);
        initialAgency.routes = Collections.singletonList(initialRoute);

        assertTrue(mAgencyDao.saveAgency(initialAgency));

        List<Route> routes = mAgencyDao.getRoutes(AGENCY_ID);
        assertNotNull(routes);
        assertEquals(routes.size(), 1);

        assertNotNull(routes.get(0).alerts);
        assertEquals(routes.get(0).alerts.size(), 1);

        assertNotNull(routes.get(0).alerts.get(0).locations);
        assertFalse(routes.get(0).alerts.get(0).locations.isEmpty());
        assertEquals(routes.get(0).alerts.get(0).locations.get(0).message, "new message");
    }

    @Test
    public void testModificationAlertUpdate() {
        mAgencyDao.saveAgency(initialAgency);
    }

    @Test
    public void testModificationAlertStale() {
        mAgencyDao.saveAgency(initialAgency);

    }

}
