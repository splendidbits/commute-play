import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AgencyDaoTest extends CommuteTestApplication {

    @BeforeClass
    public static void initialise() {
    }

    @After
    public void afterTest() {
        // Remove agency.
        mAgencyDao.removeAgency(TestModelHelper.AGENCY_ID);
    }

    @Test
    public void testDatabaseAgencyInsert() throws CloneNotSupportedException {
        // Save the agency
        Agency initialAgency = TestModelHelper.createTestAgency();
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency, check everything is still there.
        Agency updatedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
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
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Get the agency
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedAgency.name = "Updated Name";

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertEquals(fetchedAgency.name, "Updated Name");
    }

    @Test
    public void testDatabaseRouteAdd() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency has the route.
        assertNotNull(fetchedAgency.routes);
        assertEquals(fetchedAgency.routes.size(), 1);

        Route existingRoute = TestModelHelper.createTestRoute();

        // Add the second route
        Route newRoute = new Route("test_route_2");
        newRoute.externalUri = "http://example.com";
        newRoute.isSticky = false;
        newRoute.routeFlag = RouteFlag.TYPE_PRIVATE;
        newRoute.transitType = TransitType.TYPE_SPECIAL;
        newRoute.routeName = "Route 2 Name";
        newRoute.alerts = Collections.singletonList(TestModelHelper.createTestAlert());

        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedAgency.routes = Arrays.asList(existingRoute, newRoute);
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertNotNull(fetchedAgency.routes);
        assertEquals(fetchedAgency.routes.size(), 2);

        // Check it contains the previous route
        assertTrue(fetchedAgency.routes.contains(existingRoute));
        assertTrue(fetchedAgency.routes.contains(newRoute));
    }

    @Test
    public void testDatabaseRouteUpdate() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency has the route.
        assertNotNull(fetchedAgency.routes);
        assertEquals(fetchedAgency.routes.size(), 1);

        // Modify the route
        Agency updatedAgency = TestModelHelper.createTestAgency();
        Route updatedRoute = TestModelHelper.createTestRoute();
        updatedRoute.routeName = "Updated Name";
        updatedAgency.routes = Collections.singletonList(updatedRoute);

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertNotNull(fetchedAgency.routes);
        assertEquals(fetchedAgency.routes.size(), 1);

        // Check it contains the previous route
        assertTrue(fetchedAgency.routes.contains(updatedRoute));
        assertEquals(fetchedAgency.routes.get(0).routeName, "Updated Name");
    }

    @Test
    public void testDatabaseRouteRemove() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency has the route.
        assertNotNull(fetchedAgency.routes);
        assertEquals(fetchedAgency.routes.size(), 1);

        // Remove the route
        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedAgency.routes = null;
        mAgencyDao.saveAgency(updatedAgency);

        // Check the new route is not there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertTrue(fetchedAgency.routes == null || fetchedAgency.routes.isEmpty());
    }

    @Test
    public void testDatabaseAlertsAdd() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Modify route1 to have one new alert with no locations, and the existing alert
        Alert existingAlert = TestModelHelper.createTestAlert();
        Alert newAlert = new Alert();
        newAlert.highPriority = false;
        newAlert.messageTitle = "New Alert Message Title";
        newAlert.messageSubtitle = "New Alert Message Subtitle";
        newAlert.messageBody = "New Alert Message Body";
        newAlert.type = AlertType.TYPE_DETOUR;
        newAlert.locations = Collections.singletonList(TestModelHelper.createTestLocation());
        newAlert.lastUpdated = Calendar.getInstance();

        Agency updatedAgency = TestModelHelper.createTestAgency();
        Route updatedRoute = TestModelHelper.createTestRoute();
        updatedRoute.alerts = Arrays.asList(existingAlert, newAlert);
        updatedAgency.routes = Collections.singletonList(updatedRoute);

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the routes are correct.
        assertNotNull(fetchedAgency.routes);
        assertFalse(fetchedAgency.routes.isEmpty());
        assertEquals(fetchedAgency.routes.size(), 1);

        // Check the agency has the new alert.
        assertEquals(fetchedAgency.routes.get(0).alerts.size(), 2);

        assertEquals(fetchedAgency.routes.get(0).alerts.get(0).locations.size(), 1);
        assertEquals(fetchedAgency.routes.get(0).alerts.get(1).locations.size(), 1);
    }

    @Test
    public void testDatabaseAlertRemove() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        initialAgency = TestModelHelper.createTestAgency();
        Route initialRoute = TestModelHelper.createTestRoute();
        initialRoute.alerts = null;
        initialAgency.routes = Collections.singletonList(initialRoute);

        mAgencyDao.saveAgency(initialAgency);
        List<Route> routes = mAgencyDao.getRoutes(TestModelHelper.AGENCY_ID);

        assertNotNull(routes);
        assertTrue(routes.get(0).alerts == null || routes.get(0).alerts.isEmpty());
    }

    @Test
    public void testDatabaseLocationRemove() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);
        Route routes = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);

        assertNotNull(routes);
        assertNotNull(routes.alerts);
        assertEquals(routes.alerts.size(), 1);
        assertEquals(routes.alerts.get(0).locations.size(), 1);

        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedAgency.routes.get(0).alerts.get(0).locations = null;
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        Route updatedRoute = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);
        assertEquals(updatedRoute.alerts.get(0).locations.size(), 0);

        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertEquals(fetchedAgency.routes.get(0).alerts.get(0).locations.size(), 0);
    }

    @Test
    public void testDatabaseLocationUpdate() {
        Agency initialAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        Route updatedRoute = TestModelHelper.createTestRoute();

        // Update the location
        Location updatedLocation = TestModelHelper.createTestLocation();
        updatedLocation.message = "new message";

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = Collections.singletonList(updatedLocation);
        updatedAlert.route = updatedRoute;
        updatedRoute.alerts = Collections.singletonList(updatedAlert);

        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedRoute.agency = updatedAgency;
        updatedAgency.routes = Collections.singletonList(updatedRoute);

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        List<Route> routes = mAgencyDao.getRoutes(TestModelHelper.AGENCY_ID);
        assertNotNull(routes);
        assertNotNull(routes.get(0).alerts);
        assertEquals(routes.get(0).alerts.size(), 1);

        assertNotNull(routes.get(0).alerts.get(0).locations);
        assertFalse(routes.get(0).alerts.get(0).locations.isEmpty());
        assertEquals(routes.get(0).alerts.get(0).locations.get(0).message, "new message");
    }
}
