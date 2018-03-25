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

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AlertModificationsTest extends CommuteTestApplication {
    private static PushMessageManager mPushMessageManager;

    @BeforeClass
    public static void setup() {
        mPushMessageManager = application.injector().instanceOf(PushMessageManager.class);

        // Save an account
        Account testAccount = TestModelHelper.createTestAccount();
        mAccountDao.saveAccount(testAccount);

        Agency testAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(testAgency);

        Route testRoute = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);
        Device testDevice = TestModelHelper.createTestDevice();

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
            mAgencyDao.removeAgency(testAgency.id);
        }

        // Delete test Account.
        Account testAccount = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        if (testAccount != null) {
            mAccountDao.removeAccount(testAccount.id);
        }
    }

    @Test
    public void testUnchanged() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency updatedAgency = TestModelHelper.createTestAgency();

        // Populate second agency with an additional route, alerts, and locations.
        Location secondLocation = TestModelHelper.createTestLocation();
        secondLocation.message = "Second Location Test";
        secondLocation.name = "Second Location Name";
        secondLocation.date = Calendar.getInstance();

        Alert secondAlert = TestModelHelper.createTestAlert();
        secondAlert.messageTitle = "Second Alert Message Title";
        secondAlert.messageSubtitle = "Second Alert Message Subtitle";
        secondAlert.messageBody = "Second Alert Message Body";
        secondAlert.locations = Arrays.asList(TestModelHelper.createTestLocation(), secondLocation);

        Route secondRoute = TestModelHelper.createTestRoute();
        secondRoute.routeId = "test_route_2";
        secondRoute.routeName = "Second Route Name";
        secondRoute.alerts = Arrays.asList(TestModelHelper.createTestAlert(), secondAlert);

        existingAgency.routes = Arrays.asList(TestModelHelper.createTestRoute(), secondRoute);
        AlertHelper.populateBackReferences(existingAgency);

        // Populate second agency with the same data but reversed lists.
        Location updatedAgencySecondLocation = TestModelHelper.createTestLocation();
        updatedAgencySecondLocation.message = "Second Location Test";
        updatedAgencySecondLocation.name = "Second Location Name";
        updatedAgencySecondLocation.date = Calendar.getInstance();

        Alert updatedAgencySecondAlert = TestModelHelper.createTestAlert();
        updatedAgencySecondAlert.messageTitle = "Second Alert Message Title";
        updatedAgencySecondAlert.messageSubtitle = "Second Alert Message Subtitle";
        updatedAgencySecondAlert.messageBody = "Second Alert Message Body";
        updatedAgencySecondAlert.locations = Arrays.asList(TestModelHelper.createTestLocation(), updatedAgencySecondLocation);
        Collections.reverse(updatedAgencySecondAlert.locations);

        Route updatedAgencySecondRoute = TestModelHelper.createTestRoute();
        updatedAgencySecondRoute.routeId = "test_route_2";
        updatedAgencySecondRoute.routeName = "Second Route";
        updatedAgencySecondRoute.alerts = Arrays.asList(TestModelHelper.createTestAlert(), updatedAgencySecondAlert);
        Collections.reverse(updatedAgencySecondRoute.alerts);

        updatedAgency.routes = Arrays.asList(TestModelHelper.createTestRoute(), updatedAgencySecondRoute);
        Collections.reverse(updatedAgency.routes);
        AlertHelper.populateBackReferences(updatedAgency);

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
        Agency existingAgency = TestModelHelper.createTestAgency();
        existingAgency.routes.get(0).alerts = new ArrayList<>();

        Alert informationAlert = TestModelHelper.createTestAlert();
        informationAlert.messageBody = "Information alert";
        informationAlert.type = AlertType.TYPE_INFORMATION;

        // Second agency with an information alert.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Collections.singletonList(informationAlert);
        informationAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().contains(informationAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertAdd() {
        // First agency with the no alerts.
        Agency existingAgency = TestModelHelper.createTestAgency();
        existingAgency.routes.get(0).alerts = new ArrayList<>();

        Alert disruptionAlert = TestModelHelper.createTestAlert();
        disruptionAlert.messageBody = "Disruption alert";
        disruptionAlert.type = AlertType.TYPE_DISRUPTION;

        // Second agency with an disruption alert.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Collections.singletonList(disruptionAlert);
        disruptionAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().contains(disruptionAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDetourAlertAdd() {
        // First agency with the no alerts.
        Agency existingAgency = TestModelHelper.createTestAgency();
        existingAgency.routes.get(0).alerts = new ArrayList<>();

        Alert detourAlert = TestModelHelper.createTestAlert();
        detourAlert.messageBody = "Detour alert";
        detourAlert.type = AlertType.TYPE_DETOUR;

        // Second agency with a detour alert.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Collections.singletonList(detourAlert);
        detourAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().contains(detourAlert));

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertAddDisruptionAlertAdd() {
        Alert firstDisruptionAlert = TestModelHelper.createTestAlert();
        Alert secondDisruptionAlert = TestModelHelper.createTestAlert();

        firstDisruptionAlert.messageBody = "First disruption alert";
        firstDisruptionAlert.type = AlertType.TYPE_DISRUPTION;

        // First agency with the first disruption alert.
        Agency existingAgency = TestModelHelper.createTestAgency();
        existingAgency.routes.get(0).alerts = Collections.singletonList(firstDisruptionAlert);
        firstDisruptionAlert.route = existingAgency.routes.get(0);

        secondDisruptionAlert.messageBody = "Second disruption alert";
        secondDisruptionAlert.type = AlertType.TYPE_DISRUPTION;

        // Second agency with the first and second disruption alerts.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Arrays.asList(firstDisruptionAlert, secondDisruptionAlert);
        secondDisruptionAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 0 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().contains(secondDisruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testDisruptionAlertSingleRemove() {
        Alert firstDisruptionAlert = TestModelHelper.createTestAlert();
        firstDisruptionAlert.messageBody = "First disruption alert";
        firstDisruptionAlert.type = AlertType.TYPE_DISRUPTION;

        Alert secondDisruptionAlert = TestModelHelper.createTestAlert();
        secondDisruptionAlert.messageBody = "Second disruption alert";
        secondDisruptionAlert.type = AlertType.TYPE_DISRUPTION;

        // First agency with the both disruption alerts added.
        Agency existingAgency = TestModelHelper.createTestAgency();
        existingAgency.routes.get(0).alerts = Arrays.asList(firstDisruptionAlert, secondDisruptionAlert);
        firstDisruptionAlert.route = existingAgency.routes.get(0);
        secondDisruptionAlert.route = existingAgency.routes.get(0);

        // Second agency with the first disruption alert removed.
        Alert secondDisruptionAlert2 = TestModelHelper.createTestAlert();
        secondDisruptionAlert2.messageBody = "Second disruption alert 2";
        secondDisruptionAlert2.type = AlertType.TYPE_DISRUPTION;

        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Collections.singletonList(secondDisruptionAlert);
        secondDisruptionAlert2.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().contains(firstDisruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getStaleAlerts().get(0).route);

        // Check there was 1 updated alert and 0 stale alerts processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDisruptionAlertAddDisruptionAlertRemove() {
        // First agency with the first information alert.
        Agency existingAgency = TestModelHelper.createTestAgency();

        Alert disruptionAlert = TestModelHelper.createTestAlert();
        disruptionAlert.messageBody = "First disruption alert";
        disruptionAlert.type = AlertType.TYPE_DISRUPTION;

        existingAgency.routes.get(0).alerts = Collections.singletonList(disruptionAlert);
        disruptionAlert.route = existingAgency.routes.get(0);

        // Second agency with no information alerts.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = new ArrayList<>();

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().contains(disruptionAlert));

        // The route exists.
        assertNotNull(alertModifications.getStaleAlerts().get(0));
        assertNotNull(alertModifications.getStaleAlerts().get(0).route);

        // Check there was 0 updated alerts and 1 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDetourAlertAddDetourAlertRemove() {
        // First agency with the first detour alert.
        Agency existingAgency = TestModelHelper.createTestAgency();

        Alert detourAlert = TestModelHelper.createTestAlert();
        detourAlert.messageBody = "First detour alert";
        detourAlert.type = AlertType.TYPE_DETOUR;

        existingAgency.routes.get(0).alerts = Collections.singletonList(detourAlert);
        detourAlert.route = existingAgency.routes.get(0);

        // Second agency with no detour alerts.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = new ArrayList<>();

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // Check there is 0 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 0);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().contains(detourAlert));

        // Check there was 0 updated alerts and 1 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testInformationDetourAlertSwap() {
        // First agency with an information alert.
        Agency existingAgency = TestModelHelper.createTestAgency();

        Alert informationAlert = TestModelHelper.createTestAlert();
        informationAlert.messageBody = "This is an information alert";
        informationAlert.type = AlertType.TYPE_INFORMATION;

        existingAgency.routes.get(0).alerts = Collections.singletonList(informationAlert);
        informationAlert.route = existingAgency.routes.get(0);

        Alert detourAlert = TestModelHelper.createTestAlert();
        detourAlert.messageBody = "This is a detour alert";
        detourAlert.type = AlertType.TYPE_DETOUR;

        // Second agency with a detour alert.
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes.get(0).alerts = Collections.singletonList(detourAlert);
        detourAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().contains(detourAlert));
        assertTrue(alertModifications.getStaleAlerts().contains(informationAlert));

        // Check there was 1 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDetourInformationAlertSwap() {
        // First agency with a detour alert.
        Agency existingAgency = TestModelHelper.createTestAgency();

        Alert detourAlert = TestModelHelper.createTestAlert();
        detourAlert.messageBody = "This is a detour alert";
        detourAlert.type = AlertType.TYPE_DETOUR;

        existingAgency.routes.get(0).alerts = Collections.singletonList(detourAlert);
        detourAlert.route = existingAgency.routes.get(0);

        // Second agency with an information alert.
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert informationAlert = TestModelHelper.createTestAlert();
        informationAlert.messageBody = "This is an information alert";
        informationAlert.type = AlertType.TYPE_INFORMATION;

        newAgency.routes.get(0).alerts = Collections.singletonList(informationAlert);
        informationAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 1 updated alert and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().contains(informationAlert));
        assertTrue(alertModifications.getStaleAlerts().contains(detourAlert));

        // Check there was 1 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testInformationDetourAlertSwapAddDisruptionAlert() {
        // First agency with an information alert.
        Agency existingAgency = TestModelHelper.createTestAgency();

        Alert informationAlert = TestModelHelper.createTestAlert();
        informationAlert.messageBody = "This is an information alert";
        informationAlert.type = AlertType.TYPE_INFORMATION;

        existingAgency.routes.get(0).alerts = Collections.singletonList(informationAlert);
        informationAlert.route = existingAgency.routes.get(0);

        // Second agency with a detour alert and an added disruption alert.
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert detourAlert = TestModelHelper.createTestAlert();
        detourAlert.messageBody = "This is a detour alert";
        detourAlert.type = AlertType.TYPE_DETOUR;

        Alert disruptionAlert = TestModelHelper.createTestAlert();
        disruptionAlert.messageBody = "This is an disruption alert";
        disruptionAlert.type = AlertType.TYPE_DISRUPTION;

        newAgency.routes.get(0).alerts = Arrays.asList(detourAlert, disruptionAlert);
        detourAlert.route = newAgency.routes.get(0);
        disruptionAlert.route = newAgency.routes.get(0);

        // Get the modifications
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);
        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        // The route exists.
        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);

        // Check there is 2 updated alerts and 1 stale alerts.
        assertEquals(alertModifications.getUpdatedAlerts().size(), 2);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().contains(detourAlert));
        assertTrue(alertModifications.getStaleAlerts().contains(informationAlert));

        // Check there was 2 updated alerts and 0 stale alert processed.
        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 2);
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testAlertRemoveAll() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        newAgency.routes.get(0).alerts = null;
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
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Location existingLocation = TestModelHelper.createTestLocation();
        Location newLocation = TestModelHelper.createTestLocation();
        newLocation.name = "New Location";
        newLocation.date = Calendar.getInstance();
        newLocation.latitude = "-75.1653533";
        newLocation.longitude = "-75.1653533";
        newLocation.message = "New Location Message";
        newLocation.sequence = 42;

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = Arrays.asList(existingLocation, newLocation);
        newAgency.routes.get(0).alerts = Collections.singletonList(updatedAlert);

        existingLocation.alert = updatedAlert;
        newLocation.alert = updatedAlert;
        updatedAlert.route = newAgency.routes.get(0);

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertTrue(alertModifications.getUpdatedAlerts().contains(updatedAlert));
        assertEquals(alertModifications.getUpdatedAlerts().get(0).locations.size(), 2);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 1);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }

    @Test
    public void testLocationUpdate() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Location updatedLocation = TestModelHelper.createTestLocation();
        updatedLocation.name = "New Location";
        updatedLocation.message = "New Location Message";

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = Collections.singletonList(updatedLocation);

        Route updatedRoute = TestModelHelper.createTestRoute();
        updatedRoute.alerts = Collections.singletonList(updatedAlert);
        newAgency.routes = Collections.singletonList(updatedRoute);
        updatedAlert.route = updatedRoute;

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertFalse(alertModifications.getUpdatedAlerts().contains(TestModelHelper.createTestAlert()));
        assertFalse(alertModifications.getUpdatedAlerts().isEmpty());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 0);
        assertEquals(alertModifications.getUpdatedAlerts().get(0).locations.size(), 1);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getValue().size(), 0);
        assertEquals(dispatchedMessages.getKey().size(), 1);
    }

    @Test
    public void testLocationRemove() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = new ArrayList<>();

        newAgency.routes.get(0).alerts = Collections.singletonList(updatedAlert);
        updatedAlert.route = newAgency.routes.get(0);

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertFalse(alertModifications.hasChangedAlerts());

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertEquals(dispatchedMessages.getKey().size(), 0);
        assertEquals(dispatchedMessages.getValue().size(), 0);
    }
}
