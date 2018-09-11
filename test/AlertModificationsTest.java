import enums.AlertType;
import helpers.AlertHelper;
import javafx.util.Pair;
import models.AlertModifications;
import models.accounts.Account;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import models.pushservices.db.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import services.PushMessageManager;

import java.util.*;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AlertModificationsTest extends CommuteTestApplication {
    private static PushMessageManager mPushMessageManager;
    private static TestModelHelper testModelHelper;

    @BeforeClass
    public static void setup() {
        testModelHelper = new TestModelHelper(Calendar.getInstance(TimeZone.getTimeZone("EST")));
        mPushMessageManager = application.injector().instanceOf(PushMessageManager.class);

        // Save an account
        Account testAccount = testModelHelper.createTestAccount();
        mAccountDao.saveAccount(testAccount);

        Agency testAgency = testModelHelper.createTestAgency();
        mAgencyDao.saveAgency(testAgency);

        Route testRoute = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);
        Device testDevice = testModelHelper.createTestDevice();

        Subscription testSubscription = new Subscription();
        testSubscription.route = testRoute;
        testSubscription.device = testDevice;

        testDevice.account = testAccount;
        testDevice.subscriptions = Collections.singletonList(testSubscription);
        mDeviceDao.saveDevice(testDevice);
    }

    @AfterClass
    public static void teardown() {
        // Delete test Device.
        Device testDevice = mDeviceDao.getDevice(TestModelHelper.TEST_DEVICE_ID);
        if (testDevice != null) {
            mDeviceDao.removeDevice(testDevice.token);
        }

        // Delete test Agency.
        Agency testAgency = mAgencyDao.getAgency(TestModelHelper.AGENCY_ID);
        if (testAgency != null) {
            mAgencyDao.removeAgency(testAgency.getId());
        }

        // Delete test Account.
        Account testAccount = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        if (testAccount != null) {
            mAccountDao.removeAccount(testAccount.id);
        }
    }

    @Test
    public void testUnchanged() {
        Agency existingAgency = testModelHelper.createTestAgency();
        Agency updatedAgency = testModelHelper.createTestAgency();

        Alert secondAlert = testModelHelper.createTestAlert();
        secondAlert.setMessageTitle("Second Alert Message Title");
        secondAlert.setMessageSubtitle("Second Alert Message Subtitle");
        secondAlert.setMessageBody("Second Alert Message Body");
        secondAlert.setId(AlertHelper.createHash(secondAlert, "test_route_2"));

        // Populate second agency with an additional route, alerts, and locations.
        Location secondLocation = testModelHelper.createTestLocation();
        secondLocation.setMessage("Second Location Test");
        secondLocation.setName("Second Location Name");
        secondLocation.setDate(Calendar.getInstance());
        secondLocation.setId(AlertHelper.createHash(secondLocation));

        secondAlert.setLocations(Arrays.asList(testModelHelper.createTestLocation(), secondLocation));

        Route secondRoute = testModelHelper.createTestRoute("test_route_2");
        secondRoute.setRouteName("Second Route");
        secondRoute.setAlerts(Arrays.asList(testModelHelper.createTestAlert(), secondAlert));

        existingAgency.setRoutes(Arrays.asList(testModelHelper.createTestRoute(), secondRoute));

        // Populate second agency with the same data but reversed lists.
        Location updatedAgencySecondLocation = testModelHelper.createTestLocation();
        updatedAgencySecondLocation.setMessage("Second Location Test");
        updatedAgencySecondLocation.setName("Second Location Name");
        updatedAgencySecondLocation.setDate(Calendar.getInstance());

        Alert updatedAgencySecondAlert = testModelHelper.createTestAlert();
        updatedAgencySecondAlert.setMessageTitle("Second Alert Message Title");
        updatedAgencySecondAlert.setMessageSubtitle("Second Alert Message Subtitle");
        updatedAgencySecondAlert.setMessageBody("Second Alert Message Body");
        updatedAgencySecondAlert.setLocations(Arrays.asList(testModelHelper.createTestLocation(), updatedAgencySecondLocation));
        Collections.reverse(updatedAgencySecondAlert.getLocations());

        Route updatedAgencySecondRoute = testModelHelper.createTestRoute("test_route_2");
        updatedAgencySecondRoute.setRouteName("Second Route");
        updatedAgencySecondRoute.setAlerts(Arrays.asList(testModelHelper.createTestAlert(), updatedAgencySecondAlert));
        Collections.reverse(updatedAgencySecondRoute.getAlerts());

        updatedAgency.setRoutes(Arrays.asList(testModelHelper.createTestRoute(), updatedAgencySecondRoute));
        Collections.reverse(updatedAgency.getRoutes());

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, updatedAgency);

        assertNotNull(alertModifications);
        assertFalse(alertModifications.hasChangedAlerts());
        assertTrue(alertModifications.getUpdatedAlerts().isEmpty());
        assertTrue(alertModifications.getStaleAlerts().isEmpty());

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertTrue(dispatchedMessages.getKey().isEmpty());
        assertTrue(dispatchedMessages.getValue().isEmpty());
    }

    @Test
    public void testInformationAlertAdd() {
        // First agency with the no alerts.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        existingAgency.getRoutes().get(0).setAlerts(new ArrayList<>());

        Alert informationAlert = testModelHelper.createTestAlert();
        informationAlert.setMessageBody("Information alert");
        informationAlert.setType(AlertType.TYPE_INFORMATION);

        // Second agency with an information alert.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(informationAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(informationAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertAdd() {
        // First agency with the no alerts.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        existingAgency.getRoutes().get(0).setAlerts(new ArrayList<>());

        Alert disruptionAlert = testModelHelper.createTestAlert();
        disruptionAlert.setMessageBody("Disruption alert");
        disruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        // Second agency with an disruption alert.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(disruptionAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute));
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(disruptionAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDetourAlertAdd() {
        // First agency with the no alerts.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        existingAgency.getRoutes().get(0).setAlerts(new ArrayList<>());

        Alert detourAlert = testModelHelper.createTestAlert();
        detourAlert.setMessageBody("Detour alert");
        detourAlert.setType(AlertType.TYPE_DETOUR);

        // Second agency with a detour alert.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(detourAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute));
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(detourAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertAddDisruptionAlertAdd() {
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Alert firstDisruptionAlert = testModelHelper.createTestAlert();
        Alert secondDisruptionAlert = testModelHelper.createTestAlert();

        firstDisruptionAlert.setMessageBody("First disruption alert");
        firstDisruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(firstDisruptionAlert));

        secondDisruptionAlert.setMessageBody("Second disruption alert");
        secondDisruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        // Second agency with the first and second disruption alerts.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Arrays.asList(firstDisruptionAlert, secondDisruptionAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute));
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(secondDisruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertSingleRemove() {
        Alert firstDisruptionAlert = testModelHelper.createTestAlert();
        firstDisruptionAlert.setMessageBody("First disruption alert");
        firstDisruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        Alert secondDisruptionAlert = testModelHelper.createTestAlert();
        secondDisruptionAlert.setMessageBody("Second disruption alert");
        secondDisruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        // First agency with the both disruption alerts added.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);
        existingAgency.getRoutes().get(0).setAlerts(Arrays.asList(firstDisruptionAlert, secondDisruptionAlert));

        // Second agency with the first disruption alert removed.
        Alert secondDisruptionAlert2 = testModelHelper.createTestAlert();
        secondDisruptionAlert2.setMessageBody("Second disruption alert 2");
        secondDisruptionAlert2.setType(AlertType.TYPE_DISRUPTION);

        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(secondDisruptionAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().containsValue(firstDisruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getStaleAlerts().get(existingRoute).get(0));
        assertNotNull(alertModifications.getStaleAlerts().get(existingRoute).get(0));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDisruptionAlertAddDisruptionAlertRemove() {
        // First agency with the first information alert.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Alert disruptionAlert = testModelHelper.createTestAlert();
        disruptionAlert.setMessageBody("First disruption alert");
        disruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(disruptionAlert));

        // Second agency with no information alerts.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(new ArrayList<>());

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().get(existingRoute).contains(disruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getStaleAlerts().get(existingRoute).get(0));
        assertNotNull(alertModifications.getStaleAlerts().get(existingRoute).get(0));

        // Check there was 0 updated alerts and 1 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDetourAlertAddDetourAlertRemove() {
        // First agency with the first detour alert.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Alert detourAlert = testModelHelper.createTestAlert();
        detourAlert.setMessageBody("First detour alert");
        detourAlert.setType(AlertType.TYPE_DETOUR);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(detourAlert));

        // Second agency with no detour alerts.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(new ArrayList<>());

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().containsValue(detourAlert));

        // Check there was 0 updated alerts and 1 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testInformationDetourAlertSwap() {
        // First agency with an information alert.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Alert informationAlert = testModelHelper.createTestAlert();

        informationAlert.setMessageBody("This is an information alert");
        informationAlert.setType(AlertType.TYPE_INFORMATION);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(informationAlert));

        Alert detourAlert = testModelHelper.createTestAlert();
        detourAlert.setMessageBody("This is a detour alert");
        detourAlert.setType(AlertType.TYPE_DETOUR);

        // Second agency with a detour alert.
        Agency newAgency = testModelHelper.createTestAgency();
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(detourAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(existingRoute).get(0));

        // Check there is 1 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(detourAlert));
        assertTrue(alertModifications.getStaleAlerts().containsValue(informationAlert));

        // Check there was 1 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDetourInformationAlertSwap() {
        // First agency with a detour alert.
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Alert detourAlert = testModelHelper.createTestAlert();
        detourAlert.setMessageBody("This is a detour alert");
        detourAlert.setType(AlertType.TYPE_DETOUR);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(detourAlert));

        // Second agency with an information alert.
        Agency newAgency = testModelHelper.createTestAgency();

        Alert informationAlert = testModelHelper.createTestAlert();
        informationAlert.setMessageBody("This is an information alert");
        informationAlert.setType(AlertType.TYPE_INFORMATION);

        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(informationAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));

        // Check there is 1 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(informationAlert));
        assertTrue(alertModifications.getStaleAlerts().containsValue(detourAlert));

        // Check there was 1 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testInformationDetourAlertSwapAddDisruptionAlert() {
        // First agency with an information alert.
        Agency existingAgency = testModelHelper.createTestAgency();
        Alert informationAlert = testModelHelper.createTestAlert();

        informationAlert.setMessageBody("This is an information alert");
        informationAlert.setType(AlertType.TYPE_INFORMATION);

        existingAgency.getRoutes().get(0).setAlerts(Collections.singletonList(informationAlert));

        // Second agency with a detour alert and an added disruption alert.
        Agency newAgency = testModelHelper.createTestAgency();

        Alert detourAlert = testModelHelper.createTestAlert();
        detourAlert.setMessageBody("This is a detour alert");
        detourAlert.setType(AlertType.TYPE_DETOUR);

        Alert disruptionAlert = testModelHelper.createTestAlert();
        disruptionAlert.setMessageBody("This is an disruption alert");
        disruptionAlert.setType(AlertType.TYPE_DISRUPTION);

        newAgency.getRoutes().get(0).setAlerts(Arrays.asList(detourAlert, disruptionAlert));

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));

        // Check there is 2 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 2);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(detourAlert));
        assertTrue(alertModifications.getStaleAlerts().containsValue(informationAlert));

        // Check there was 2 updated alerts and 1 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 2);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testAlertRemoveAll() {
        Agency existingAgency = testModelHelper.createTestAgency();
        Agency newAgency = testModelHelper.createTestAgency();

        newAgency.getRoutes().get(0).setAlerts(null);
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);

        // Check there was 1 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testLocationAdd() {
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);
        Agency newAgency = testModelHelper.createTestAgency();

        Location existingLocation = testModelHelper.createTestLocation();
        Location newLocation = testModelHelper.createTestLocation();
        newLocation.setName("New Location");
        newLocation.setDate(Calendar.getInstance());
        newLocation.setLongitude("-75.1653533");
        newLocation.setLongitude("-75.1653533");
        newLocation.setMessage("New Location Message");
        newLocation.setSequence(42);

        Alert updatedAlert = testModelHelper.createTestAlert();
        updatedAlert.setLocations(Arrays.asList(existingLocation, newLocation));
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(updatedAlert));

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().containsValue(updatedAlert));
        assertEquals(alertModifications.getUpdatedAlerts().get(existingRoute).get(0).getLocations().size(), 2);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testLocationUpdate() {
        Agency existingAgency = testModelHelper.createTestAgency();
        final Route existingRoute = existingAgency.getRoutes().get(0);

        Agency newAgency = testModelHelper.createTestAgency();

        Location updatedLocation = testModelHelper.createTestLocation();
        updatedLocation.setName("New Location");
        updatedLocation.setMessage("New Location Message");

        Alert updatedAlert = testModelHelper.createTestAlert();
        updatedAlert.setLocations(Collections.singletonList(updatedLocation));

        Route updatedRoute = testModelHelper.createTestRoute();
        updatedRoute.setAlerts(Collections.singletonList(updatedAlert));
        newAgency.setRoutes(Collections.singletonList(updatedRoute));

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertFalse(alertModifications.getUpdatedAlerts().containsValue(testModelHelper.createTestAlert()));
        assertFalse(alertModifications.getUpdatedAlerts().isEmpty());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertEquals(alertModifications.getUpdatedAlerts().get(existingRoute).get(0).getLocations().size(), 1);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getValue().size(), 0);
        assertEquals(dispatchedMessages.getKey().size(), 1);
    }

    @Test
    public void testLocationRemove() {
        Agency existingAgency = testModelHelper.createTestAgency();
        Agency newAgency = testModelHelper.createTestAgency();

        Alert updatedAlert = testModelHelper.createTestAlert();
        updatedAlert.setLocations(new ArrayList<>());
        newAgency.getRoutes().get(0).setAlerts(Collections.singletonList(updatedAlert));

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertFalse(alertModifications.hasChangedAlerts());

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }
}
