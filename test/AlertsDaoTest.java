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
public class AlertsDaoTest extends CommuteTestApplication {
    private static AgencyDao mAgencyDao;

    private final Calendar locationDate = Calendar.getInstance();
    private final static int AGENCY_ID = 999;
    private final static String ROUTE1_ID = "test_route_1";

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
        initialLocation.id = null;
        initialLocation.latitude = "-75.1653533";
        initialLocation.longitude = "39.9517899";
        initialLocation.message = "Location Message";
        initialLocation.name = "Location Name";
        initialLocation.sequence = 100;
        initialLocation.date = locationDate;

        initialAlert = new Alert();
        initialAlert.id = null;
        initialAlert.highPriority = false;
        initialAlert.messageTitle = "Alert Message Title";
        initialAlert.messageSubtitle = "Alert Message Subtitle";
        initialAlert.messageBody = "Alert Message Body";
        initialAlert.type = AlertType.TYPE_INFORMATION;
        initialAlert.lastUpdated = locationDate;
        initialAlert.locations = new ArrayList<>();
        initialAlert.locations.add(initialLocation);

        initialRoute = new Route(ROUTE1_ID);
        initialRoute.id = null;
        initialRoute.externalUri = "http://example.com";
        initialRoute.isSticky = false;
        initialRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        initialRoute.transitType = TransitType.TYPE_SPECIAL;
        initialRoute.routeName = "Route Name";
        initialRoute.alerts = new ArrayList<>();
        initialRoute.alerts.add(initialAlert);

        initialAgency = new Agency(AGENCY_ID);
        initialAgency.id = AGENCY_ID;
        initialAgency.name = "Test Agency";
        initialAgency.externalUri = "http://example.com";
        initialAgency.phone = "123-123-1234";
        initialAgency.utcOffset = -5F;
        initialAgency.routes = new ArrayList<>();
        initialAgency.routes.add(initialRoute);
    }

    @After
    public void afterTest() {
        // Remove agency.
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
        final String ROUTE2_ID = "test_route_2";
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Add the second route
        Route addedRoute = new Route(ROUTE2_ID);
        addedRoute.externalUri = "http://example.com";
        addedRoute.isSticky = false;
        addedRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        addedRoute.transitType = TransitType.TYPE_SPECIAL;
        addedRoute.routeName = "Route 2 Name";
        initialAgency.routes.add(addedRoute);
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Check the new route is there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 2);

        // Check it contains the previous route
        assertTrue(updatedAgency.routes.contains(addedRoute));
    }

    @Test
    public void testDatabaseRouteUpdate() {
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Modify the route
        Route route = new Route(ROUTE1_ID);
        route.externalUri = "http://example.com";
        route.isSticky = false;
        route.routeFlag = RouteFlag.TYPE_PRIVATE;
        route.transitType = TransitType.TYPE_SPECIAL;
        route.routeName = "Updated Name";
        initialAgency.routes = Collections.singletonList(route);
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Check the new route is there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Check it contains the previous route
        assertTrue(updatedAgency.routes.contains(route));
        assertEquals(updatedAgency.routes.get(0).routeName, "Updated Name");
    }

    @Test
    public void testDatabaseRouteRemove() {
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the agency has the route.
        assertNotNull(updatedAgency.routes);
        assertEquals(updatedAgency.routes.size(), 1);

        // Remove the route
        updatedAgency.routes = null;
        mAgencyDao.saveAgency(updatedAgency);

        // Check the new route is not there.
        updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);
        assertTrue(updatedAgency.routes == null || updatedAgency.routes.isEmpty());
    }

    @Test
    public void testDatabaseAlertsAdd() {
        mAgencyDao.saveAgency(initialAgency);

        // Modify route1 to have one new alert with no locations, and the existing alert
        Alert newAlert1 = new Alert();
        newAlert1.highPriority = false;
        newAlert1.messageTitle = "New Alert Message Title";
        newAlert1.messageSubtitle = "New Alert Message Subtitle";
        newAlert1.messageBody = "New Alert Message Body";
        newAlert1.type = AlertType.TYPE_DETOUR;
        newAlert1.lastUpdated = Calendar.getInstance();
        initialRoute.alerts.add(newAlert1);

        initialAgency.routes = Collections.singletonList(initialRoute);
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        Agency updatedAgency = mAgencyDao.getAgency(AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the routes are correct.
        assertNotNull(updatedAgency.routes);
        assertFalse(updatedAgency.routes.isEmpty());
        assertEquals(updatedAgency.routes.size(), 1);

        // Check that the alerts have persisted
        List<Alert> allAlerts = new ArrayList<>();
        for (Route updatedRoute : updatedAgency.routes) {
            if (updatedRoute.alerts != null) {
                allAlerts.addAll(updatedRoute.alerts);
            }
        }

        // Check the agency has the new alert.
        assertEquals(allAlerts.size(), 2);

        // Check the alert has the same location.
        List<Location> updatedLocations = new ArrayList<>();
        for (Alert alert : allAlerts) {
            if (alert.locations != null) {
                updatedLocations.addAll(alert.locations);
            }
        }

        assertEquals(updatedLocations.size(), 1);
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
}
