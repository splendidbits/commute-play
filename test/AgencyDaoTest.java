import enums.AlertType;
import enums.TransitType;
import helpers.AlertHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AgencyDaoTest extends CommuteTestApplication {
    private static TestModelHelper testModelHelper;

    @BeforeClass
    public static void initialise() {
        testModelHelper = new TestModelHelper(Calendar.getInstance(TimeZone.getTimeZone("EST")));
    }

    @After
    public void afterTest() {
        // Remove agency.
        mAgencyDao.removeAgency(TestModelHelper.AGENCY_ID);
    }

    @Test
    public void testDatabaseAgencyInsert() {
        // Save the agency
        Agency initialAgency = testModelHelper.createTestAgency();
        assertTrue(mAgencyDao.saveAgency(initialAgency));

        // Get the agency, check everything is still there.
        Agency updatedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(updatedAgency);

        // Check the agency has the route.
        assertNotNull(updatedAgency.getRoutes());
        assertEquals(1, updatedAgency.getRoutes().size());

        // Check the agency has the Alert.
        assertNotNull(updatedAgency.getRoutes().get(0));
        assertNotNull(updatedAgency.getRoutes().get(0).getAlerts());
        assertEquals(1, updatedAgency.getRoutes().get(0).getAlerts().size());

        // Check the agency has the Location.
        assertNotNull(updatedAgency.getRoutes().get(0).getAlerts().get(0).getLocations());
        assertEquals(1, updatedAgency.getRoutes().get(0).getAlerts().get(0).getLocations().size());
    }

    @Test
    public void testDatabaseAgencyUpdate() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Get the agency
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        Agency updatedAgency = testModelHelper.createTestAgency();
        updatedAgency.setName("Updated Name");

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertEquals(fetchedAgency.getName(), "Updated Name");
    }

    @Test
    public void testDatabaseRouteAdd() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency
        assertNotNull(fetchedAgency.getRoutes());
        assertEquals(1, fetchedAgency.getRoutes().size());
        assertEquals(1, fetchedAgency.getRoutes().get(0).getAlerts().size());
        assertEquals(1, fetchedAgency.getRoutes().get(0).getAlerts().get(0).getLocations().size());

        // Add original and new route
        Route routeOne = testModelHelper.createTestRoute();

        Alert routeTwoAlert = testModelHelper.createTestAlert();
        routeTwoAlert.setMessageBody("new alert");

        Location routeTwoLocation = testModelHelper.createTestLocation();
        routeTwoLocation.setName("new location");
        routeTwoLocation.setId(AlertHelper.createHash(routeTwoLocation));
        routeTwoAlert.setLocations(new ArrayList<>(singletonList(routeTwoLocation)));

        routeTwoAlert.setId(AlertHelper.createHash(routeTwoAlert, "route_2"));
        routeTwoAlert.setLocations(new ArrayList<>(singletonList(routeTwoLocation)));


        Route routeTwo = testModelHelper.createTestRoute("route_2");
        routeTwo.setRouteName("route_2");
        routeTwo.setAlerts(new ArrayList<>(singletonList(routeTwoAlert)));
        routeTwo.setTransitType(TransitType.TYPE_SPECIAL);

        Agency updatedAgency = testModelHelper.createTestAgency();
        updatedAgency.setRoutes(Arrays.asList(routeOne, routeTwo));
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertNotNull(fetchedAgency.getRoutes());
        assertEquals(2, fetchedAgency.getRoutes().size());

        // Check it contains the previous route
        assertTrue(fetchedAgency.getRoutes().contains(routeOne));
        assertTrue(fetchedAgency.getRoutes().contains(routeTwo));
    }

    @Test
    public void testDatabaseRouteUpdate() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency has the route.
        assertNotNull(fetchedAgency.getRoutes());
        assertEquals(1, fetchedAgency.getRoutes().size());

        // Modify the route
        Agency updatedAgency = testModelHelper.createTestAgency();
        Route updatedRoute = testModelHelper.createTestRoute();
        updatedRoute.setRouteName("Updated Name");
        updatedAgency.setRoutes(new ArrayList<>(singletonList(updatedRoute)));

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        // Check the new route is there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertNotNull(fetchedAgency.getRoutes());
        assertEquals(1, fetchedAgency.getRoutes().size());

        // Check it contains the previous route
        assertTrue(fetchedAgency.getRoutes().contains(updatedRoute));
        assertEquals("Updated Name", fetchedAgency.getRoutes().get(0).getRouteName());
    }

    @Test
    public void testDatabaseRouteRemove() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Ensure the Agency was updated
        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the agency has the route.
        assertNotNull(fetchedAgency.getRoutes());
        assertEquals(1, fetchedAgency.getRoutes().size());

        // Remove the route
        Agency updatedAgency = testModelHelper.createTestAgency();
        updatedAgency.setRoutes(null);
        mAgencyDao.saveAgency(updatedAgency);

        // Check the new route is not there.
        fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);
        assertTrue(fetchedAgency.getRoutes() == null || fetchedAgency.getRoutes().isEmpty());
    }

    @Test
    public void testDatabaseAlertsAdd() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        // Modify route1 to have one new alert with no locations, and the existing alert
        Alert existingAlert = testModelHelper.createTestAlert();
        existingAlert.setId(AlertHelper.createHash(existingAlert, TestModelHelper.ROUTE_ID));

        Alert newAlert = new Alert();
        newAlert.setHighPriority(false);
        newAlert.setMessageTitle("New Alert Message Title");
        newAlert.setMessageSubtitle("New Alert Message Subtitle");
        newAlert.setMessageBody("New Alert Message Body");
        newAlert.setType(AlertType.TYPE_DETOUR);
        newAlert.setLastUpdated(Calendar.getInstance());
        newAlert.setId(AlertHelper.createHash(newAlert, TestModelHelper.ROUTE_ID));

        newAlert.setLocations(new ArrayList<>(singletonList(testModelHelper.createTestLocation())));

        Agency updatedAgency = testModelHelper.createTestAgency();
        Route updatedRoute = testModelHelper.createTestRoute();
        updatedRoute.setAlerts(Arrays.asList(existingAlert, newAlert));
        updatedAgency.setRoutes(new ArrayList<>(singletonList(updatedRoute)));

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertNotNull(fetchedAgency);

        // Check the routes are correct.
        assertNotNull(fetchedAgency.getRoutes());
        assertFalse(fetchedAgency.getRoutes().isEmpty());
        assertEquals(1, fetchedAgency.getRoutes().size());

        // Check the agency has the new alert.
        assertEquals(2, fetchedAgency.getRoutes().get(0).getAlerts().size());
        assertEquals(1, fetchedAgency.getRoutes().get(0).getAlerts().get(0).getLocations().size());
        assertEquals(1, fetchedAgency.getRoutes().get(0).getAlerts().get(1).getLocations().size());
    }

    @Test
    public void testDatabaseAlertRemove() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        initialAgency = testModelHelper.createTestAgency();
        Route initialRoute = testModelHelper.createTestRoute();
        initialRoute.setAlerts(null);
        initialAgency.setRoutes(new ArrayList<>(singletonList(initialRoute)));

        mAgencyDao.saveAgency(initialAgency);
        List<Route> routes = mAgencyDao.getRoutes(TestModelHelper.AGENCY_ID);

        assertNotNull(routes);
        assertTrue(routes.get(0).getAlerts() == null || routes.get(0).getAlerts().isEmpty());
    }

    @Test
    public void testDatabaseLocationRemove() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);
        Route routes = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);

        assertNotNull(routes);
        assertNotNull(routes.getAlerts());
        assertEquals(1, routes.getAlerts().size());
        assertEquals(1, routes.getAlerts().get(0).getLocations().size());

        Agency updatedAgency = testModelHelper.createTestAgency();
        updatedAgency.getRoutes().get(0).getAlerts().get(0).setLocations(null);
        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        Route updatedRoute = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);
        assertEquals(0, updatedRoute.getAlerts().get(0).getLocations().size());

        Agency fetchedAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        assertEquals(0, fetchedAgency.getRoutes().get(0).getAlerts().get(0).getLocations().size());
    }

    @Test
    public void testDatabaseLocationUpdate() {
        Agency initialAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(initialAgency);

        Route updatedRoute = testModelHelper.createTestRoute();
        Alert updatedAlert = testModelHelper.createTestAlert();

        // Update the location
        int locationId = initialAgency.getRoutes().get(0).getAlerts().get(0).getLocations().get(0).getId();
        Location updatedLocation = testModelHelper.createTestLocation();
        updatedLocation.setId(locationId);
        updatedLocation.setMessage("new message");

        updatedAlert.setLocations(new ArrayList<>(singletonList(updatedLocation)));
        updatedRoute.setAlerts(new ArrayList<>(singletonList(updatedAlert)));

        Agency updatedAgency = testModelHelper.createTestAgency();
        updatedAgency.setRoutes(new ArrayList<>(singletonList(updatedRoute)));

        assertTrue(mAgencyDao.saveAgency(updatedAgency));

        List<Route> routes = mAgencyDao.getRoutes(TestModelHelper.AGENCY_ID);
        assertNotNull(routes);
        assertNotNull(routes.get(0).getAlerts());
        assertEquals(1, routes.get(0).getAlerts().size());

        assertNotNull(routes.get(0).getAlerts().get(0).getLocations());
        assertFalse(routes.get(0).getAlerts().get(0).getLocations().isEmpty());
        assertEquals("new message", routes.get(0).getAlerts().get(0).getLocations().get(0).getMessage());
    }
}
