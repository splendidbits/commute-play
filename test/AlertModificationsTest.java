import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import helpers.AlertHelper;
import models.AlertModifications;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AlertModificationsTest {
    private static final Calendar LOCATION_DATE = Calendar.getInstance();

    @Before
    public void beforeTest() {

    }

    @After
    public void afterTest() {

    }

    @Test
    public void testModificationUnchanged() {
        Agency existingAgency = createTestAgency();
        Agency newAgency = createTestAgency();
        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertFalse(alertModifications.hasUpdatedRoutes());
        assertNotNull(alertModifications.getUpdatedRoutes());
        assertTrue(alertModifications.getUpdatedRoutes().isEmpty());
    }

    @Test
    public void testModificationRouteAdd() {
        Agency existingAgency = createTestAgency();
        Agency updatedAgency = createTestAgency();

        Route existingRoute = createTestRoute();
        existingRoute.alerts = Collections.singletonList(createTestAlert());

        List<Alert> freshAlerts = new ArrayList<>();
        freshAlerts.add(createTestAlert());

        List<Route> newRoutes = new ArrayList<>();
        Route newRoute = createTestRoute();
        newRoute.id = "updated_route";
        newRoute.routeId = "updated_route";
        newRoute.alerts = freshAlerts;

        newRoutes.add(existingRoute);
        newRoutes.add(newRoute);

        updatedAgency.routes = newRoutes;

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, updatedAgency);

        assertTrue(alertModifications.hasUpdatedRoutes());
        assertNotNull(alertModifications.getUpdatedRoutes());

        assertTrue(alertModifications.getStaleRoutes().isEmpty());
        assertEquals(alertModifications.getUpdatedRoutes().size(), 1);
        assertTrue(alertModifications.getUpdatedRoutes().contains(newRoute));
    }

    @Test
    public void testModificationRouteUpdate() {

    }

    @Test
    public void testModificationRouteRemove() {

    }

    @Test
    public void testModificationAlertAdd() {
        Agency existingAgency = createTestAgency();
        Agency newAgency = createTestAgency();

        Alert originalAlert = createTestAlert();
        Alert newAlert = createTestAlert();
        newAlert.messageBody = "This is a new alert";

        List<Alert> newAlerts = new ArrayList<>();
        newAlerts.add(originalAlert);
        newAlerts.add(newAlert);
        newAgency.routes.get(0).alerts = newAlerts;

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasUpdatedRoutes());

        assertNotNull(alertModifications.getUpdatedRoutes().get(0).alerts);
        assertEquals(alertModifications.getUpdatedRoutes().get(0).alerts.size(), 2);
        assertTrue(alertModifications.getUpdatedRoutes().get(0).alerts.contains(originalAlert));
        assertTrue(alertModifications.getUpdatedRoutes().get(0).alerts.contains(newAlert));
    }

    @Test
    public void testModificationAlertUpdate() {
        Agency existingAgency = createTestAgency();
        Agency newAgency = createTestAgency();

        Alert updatedAlert = createTestAlert();
        updatedAlert.messageTitle = "new title";
        newAgency.routes.get(0).alerts = Collections.singletonList(updatedAlert);

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasUpdatedRoutes());
        assertNotNull(alertModifications.getUpdatedRoutes().get(0).alerts);
        assertEquals(alertModifications.getUpdatedRoutes().get(0).alerts.size(), 1);
        assertTrue(alertModifications.getUpdatedRoutes().get(0).alerts.contains(updatedAlert));
    }

    @Test
    public void testModificationAlertRemove() {
        Agency existingAgency = createTestAgency();
        Agency newAgency = createTestAgency();
        newAgency.routes = new ArrayList<>();

        AlertModifications alertModifications = AlertHelper.getAgencyModifications(existingAgency, newAgency);

        assertNotNull(alertModifications);
        assertTrue(alertModifications.hasUpdatedRoutes());
        assertFalse(alertModifications.getUpdatedRoutes().contains(createTestRoute()));
        assertTrue(alertModifications.getUpdatedRoutes().isEmpty());

        assertFalse(alertModifications.getStaleRoutes().isEmpty());

    }

    @Test
    public void testModificationLocationAdd() {

    }

    @Test
    public void testModificationLocationUpdate() {

    }

    @Test
    public void testModificationLocationRemove() {

    }

    @Nonnull
    private Location createTestLocation() {
        Location location = new Location();
        location.id = null;
        location.latitude = "-75.1653533";
        location.longitude = "39.9517899";
        location.message = "Location Test";
        location.name = "Location Name";
        location.sequence = 100;
        location.date = LOCATION_DATE;

        return location;
    }

    @Nonnull
    private Alert createTestAlert() {
        Alert alert = new Alert();
        alert.id = 12345L;
        alert.highPriority = false;
        alert.messageTitle = "Alert Message Title";
        alert.messageSubtitle = "Alert Message Subtitle";
        alert.messageBody = "Alert Message Body";
        alert.type = AlertType.TYPE_INFORMATION;
        alert.lastUpdated = Calendar.getInstance();
        alert.locations = new ArrayList<>();

        return alert;
    }

    @Nonnull
    private Route createTestRoute() {
        Route route = new Route("test_route_1");
        route.id = "test_route_1";
        route.externalUri = "http://example.com";
        route.isSticky = false;
        route.routeFlag = RouteFlag.TYPE_PRIVATE;
        route.transitType = TransitType.TYPE_SPECIAL;
        route.routeName = "Route Name";
        route.alerts = new ArrayList<>();

        return route;
    }

    @Nonnull
    private Agency createTestAgency() {
        Alert alert = createTestAlert();
        alert.locations = Collections.singletonList(createTestLocation());
        Route route = createTestRoute();
        route.alerts = Collections.singletonList(alert);

        Agency agency = new Agency(999);
        agency.name = "Test Agency";
        agency.externalUri = "http://example.com";
        agency.phone = "123-123-1234";
        agency.utcOffset = -5F;
        agency.routes = Collections.singletonList(route);

        return agency;
    }

}
