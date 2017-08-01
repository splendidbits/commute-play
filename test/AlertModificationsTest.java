import enums.AlertType;
import helpers.AlertHelper;
import models.AlertModifications;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AlertModificationsTest extends CommuteTestApplication {

    @Test
    public void testModificationUnchanged() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertFalse(alertModifications.hasChangedAlerts());
        assertNotNull(alertModifications.getUpdatedAlerts());
        assertTrue(alertModifications.getUpdatedAlerts().isEmpty());
    }

    @Test
    public void testModificationAlertAdd() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert originalAlert = TestModelHelper.createTestAlert();
        originalAlert.route = TestModelHelper.createTestRoute();

        Alert newAlert = TestModelHelper.createTestAlert();
        newAlert.route = TestModelHelper.createTestRoute();
        newAlert.messageBody = "This is a new alert";

        List<Alert> newAlerts = new ArrayList<>();
        newAlerts.add(originalAlert);
        newAlerts.add(newAlert);
        newAgency.routes.get(0).alerts = newAlerts;

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());

        assertNotNull(alertModifications.getUpdatedAlerts().get(0));
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);
        assertEquals(alertModifications.getUpdatedAlerts().size(), 2);
        assertTrue(alertModifications.getUpdatedAlerts().contains(originalAlert));
        assertTrue(alertModifications.getUpdatedAlerts().contains(newAlert));
    }

    @Test
    public void testModificationAlertUpdate() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.messageTitle = "new title";
        updatedAlert.route = TestModelHelper.createTestRoute();
        newAgency.routes.get(0).alerts = Collections.singletonList(updatedAlert);

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertNotNull(alertModifications.getUpdatedAlerts());
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().contains(updatedAlert));
    }

    @Test
    public void testModificationAlertTypeSwap() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.type = AlertType.TYPE_DETOUR;
        updatedAlert.route = TestModelHelper.createTestRoute();
        newAgency.routes.get(0).alerts = Collections.singletonList(updatedAlert);

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertNotNull(alertModifications.getUpdatedAlerts());
        assertNotNull(alertModifications.getUpdatedAlerts().get(0).route);
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertEquals(alertModifications.getStaleAlerts().size(), 1);
        assertTrue(alertModifications.getUpdatedAlerts().contains(updatedAlert));
    }

    @Test
    public void testModificationAlertRemove() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();
        newAgency.routes = new ArrayList<>();

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertFalse(alertModifications.getUpdatedAlerts().contains(TestModelHelper.createTestAlert()));
        assertTrue(alertModifications.getUpdatedAlerts().isEmpty());
        assertFalse(alertModifications.getStaleAlerts().isEmpty());
    }

    @Test
    public void testModificationLocationAdd() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Location originalLocation = TestModelHelper.createTestLocation();

        Location newLocation = TestModelHelper.createTestLocation();
        newLocation.name = "New Location";
        newLocation.date = Calendar.getInstance();
        newLocation.latitude = originalLocation.latitude;
        newLocation.longitude = originalLocation.longitude;
        newLocation.message = "New Location Message";
        newLocation.sequence = 42;

        List<Location> locationList = new ArrayList<>();
        locationList.add(originalLocation);
        locationList.add(newLocation);

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = locationList;

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
        assertTrue(alertModifications.getStaleAlerts().isEmpty());
        assertEquals(alertModifications.getUpdatedAlerts().get(0).locations.size(), 2);
    }

    @Test
    public void testModificationLocationUpdate() {
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
        assertTrue(alertModifications.getStaleAlerts().isEmpty());
        assertEquals(alertModifications.getUpdatedAlerts().get(0).locations.size(), 1);
    }

    @Test
    public void testModificationLocationRemove() {
        Agency existingAgency = TestModelHelper.createTestAgency();
        Agency newAgency = TestModelHelper.createTestAgency();

        Alert updatedAlert = TestModelHelper.createTestAlert();
        updatedAlert.locations = new ArrayList<>();

        Route updatedRoute = TestModelHelper.createTestRoute();
        updatedRoute.alerts = Collections.singletonList(updatedAlert);
        newAgency.routes = Collections.singletonList(updatedRoute);
        updatedAlert.route = updatedRoute;

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasChangedAlerts());
        assertTrue(alertModifications.getUpdatedAlerts().contains(updatedAlert));
        assertFalse(alertModifications.getUpdatedAlerts().isEmpty());
        assertEquals(alertModifications.getUpdatedAlerts().size(), 1);
        assertTrue(alertModifications.getStaleAlerts().isEmpty());
        assertTrue(alertModifications.getUpdatedAlerts().get(0).locations.isEmpty());
    }
}