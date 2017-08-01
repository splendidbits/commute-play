import helpers.AlertHelper;
import models.AlertModifications;
import models.alerts.Agency;
import models.alerts.Alert;
import org.junit.Test;

import java.util.ArrayList;
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
        assertFalse(true);
    }

    @Test
    public void testModificationLocationUpdate() {
        assertFalse(true);
    }

    @Test
    public void testModificationLocationRemove() {
        assertFalse(true);
    }
}
