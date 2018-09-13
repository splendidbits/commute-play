package main;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import enums.AlertType;
import enums.TransitType;
import enums.pushservices.PlatformType;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 01/08/2017 Splendid Bits.
 */
public class TestModelHelper {
    private Calendar alertLocationDate = Calendar.getInstance(TimeZone.getTimeZone("EST"));

    static final String AGENCY_ID = "TEST";
    static final String ROUTE_ID = "test_route_1";
    static final String TEST_DEVICE_ID = "test_id";
    static final String TEST_DEVICE_TOKEN = "test_token_123456";
    static final String ACCOUNT_API_KEY = "test_api_key";

    public TestModelHelper(Calendar withDate) {
        if (withDate != null) {
            alertLocationDate = withDate;
        }
    }

    @Nonnull
    public Location createTestLocation() {
        Location location = new Location();
        location.setLatitude("-75.1653533");
        location.setLongitude("39.9517899");
        location.setMessage("Location Test");
        location.setName("Location Name");
        location.setSequence(100);
        location.setDate(alertLocationDate);

        return location;
    }

    @Nonnull
    public Alert createTestAlert() {
        Alert alert = new Alert();
        alert.setHighPriority(false);
        alert.setMessageTitle("Alert Message Title");
        alert.setMessageSubtitle("Alert Message Subtitle");
        alert.setMessageBody("Alert Message Body");
        alert.setType(AlertType.TYPE_INFORMATION);
        alert.setLastUpdated(alertLocationDate);

        Location location = createTestLocation();
        alert.setLocations(Collections.singletonList(location));
        return alert;
    }

    @Nonnull
    public Route createTestRoute() {
        return createTestRoute(ROUTE_ID);
    }

    @Nonnull
    public Route createTestRoute(String id) {
        Route route = new Route();
        route.setRouteId(id);
        route.setExternalUri("http://example.com");
        route.setSticky(false);
        route.setTransitType(TransitType.TYPE_SPECIAL);
        route.setRouteName("Route Name");

        Alert alert = createTestAlert();
        route.setAlerts(Collections.singletonList(alert));
        return route;
    }

    @Nonnull
    public Agency createTestAgency() {
        Agency agency = new Agency(AGENCY_ID);
        agency.setName("Test Agency");
        agency.setExternalUri("http://example.com");
        agency.setPhone("123-123-1234");
        agency.setUtcOffset(-5F);

        Route route = createTestRoute(ROUTE_ID);
        agency.setRoutes(Collections.singletonList(route));
        return agency;
    }

    @Nonnull
    public Account createTestAccount() {
        PlatformAccount platformAccount = new PlatformAccount();
        platformAccount.authorisationKey = "test_auth_key";
        platformAccount.packageUri = "com.staticfish.commute.test";
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
    public Device createTestDevice() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.timeRegistered = new Date();
        return device;
    }
}
