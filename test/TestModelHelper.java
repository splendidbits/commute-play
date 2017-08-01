import enums.AlertType;
import enums.RouteFlag;
import enums.TransitType;
import enums.pushservices.PlatformType;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 01/08/2017 Splendid Bits.
 */
public class TestModelHelper {
    static final Calendar LOCATION_DATE = Calendar.getInstance();
    static final int AGENCY_ID = 999;
    static final String ROUTE_ID = "test_route_1";
    static final String TEST_DEVICE_ID = "test_id";
    static final String TEST_DEVICE_TOKEN = "test_token_123456";
    static final String ACCOUNT_API_KEY = "test_api_key";

    @Nonnull
    static Location createTestLocation() {
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
    static Alert createTestAlert() {
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
    static Route createTestRoute() {
        Route route = new Route(ROUTE_ID);
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
    static Agency createTestAgency() {
        Alert alert = createTestAlert();
        alert.locations = Collections.singletonList(createTestLocation());
        Route route = createTestRoute();
        alert.route = route;
        route.alerts = Collections.singletonList(alert);

        Agency agency = new Agency(AGENCY_ID);
        agency.name = "Test Agency";
        agency.externalUri = "http://example.com";
        agency.phone = "123-123-1234";
        agency.utcOffset = -5F;
        agency.routes = Collections.singletonList(route);

        return agency;
    }

    @Nonnull
    static Account createTestAccount() {
        PlatformAccount platformAccount = new PlatformAccount();
        platformAccount.authorisationKey = "test_auth_key";
        platformAccount.platformType = PlatformType.SERVICE_GCM;

        // Save the account
        Account newAccount = new Account();
        newAccount.apiKey = ACCOUNT_API_KEY;
        newAccount.orgName = "Test Organisation";
        newAccount.active = false;
        newAccount.email = "test@example.com";
        newAccount.dailyEstLimit = 51200L;
        newAccount.platformAccounts = Collections.singletonList(platformAccount);

        return newAccount;
    }

    @Nonnull
    static Device createTestDevice() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.timeRegistered = new Date();
        return device;
    }
}
