import models.accounts.Account;
import models.alerts.Agency;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class DeviceDaoTest extends CommuteTestApplication {

    @BeforeClass
    public static void initialise() {
        // Save an account
        Account testAccount = TestModelHelper.createTestAccount();
        mAccountDao.saveAccount(testAccount);

        // Save an agency
        Agency testAgency = TestModelHelper.createTestAgency();
        mAgencyDao.saveAgency(testAgency);
    }

    @AfterClass
    public static void shutdown() {
        // Delete Agency
        mAgencyDao.removeAgency(TestModelHelper.AGENCY_ID);

        // Delete account
        Account testAccount = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        mAccountDao.removeAccount(testAccount.id);
    }


    @After
    public void afterTest() {
        mDeviceDao.removeDevice(TestModelHelper.TEST_DEVICE_TOKEN);
    }

    @Test
    public void testDatabaseDeviceInsert() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        assertTrue(mDeviceDao.saveDevice(initialDevice));
        Device existingDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(existingDevice);
    }

    @Test
    public void testDatabaseDeviceUpdate() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        assertTrue(mDeviceDao.saveDevice(initialDevice));

        Device existingDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(existingDevice);

        String newDeviceToken = "updated_test_token";
        Device newDevice = TestModelHelper.createTestDevice();
        newDevice.token = newDeviceToken;
        newDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        assertTrue(mDeviceDao.saveDevice(newDevice));

        existingDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(existingDevice);
        assertNotNull(existingDevice.token);
        assertEquals(existingDevice.token, newDeviceToken);
        assertNotNull(existingDevice.subscriptions);
        assertTrue(existingDevice.subscriptions.isEmpty());

        mDeviceDao.removeDevice(newDeviceToken);
    }

    @Test
    public void testDatabaseDeviceTokenUpdate() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        // Change device token to new token id.
        mDeviceDao.saveDevice(initialDevice);

        String newDeviceToken = "updated_test_token";
        Device updatedDevice = TestModelHelper.createTestDevice();
        updatedDevice.token = newDeviceToken;
        assertTrue(mDeviceDao.saveUpdatedToken(TestModelHelper.TEST_DEVICE_TOKEN, updatedDevice.token));

        // Get the changed device.
        Device fetchedDevice = mDeviceDao.getDevice(TestModelHelper.TEST_DEVICE_ID);
        assertNotNull(fetchedDevice);
        assertEquals(fetchedDevice.token, newDeviceToken);

        // Change token back so it can be deleted.
        mDeviceDao.removeDevice(newDeviceToken);
    }

    @Test
    public void testDatabaseDeviceRemove() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        // Add test device.
        mDeviceDao.saveDevice(initialDevice);

        // Remove device.
        assertTrue(mDeviceDao.removeDevice(initialDevice.token));
        assertNull(mDeviceDao.getDevice(initialDevice.deviceId));
    }

    @Test
    public void testDatabaseSubscriptionSave() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        Route route = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);
        Subscription subscription = new Subscription();
        subscription.route = route;

        initialDevice.id = null;
        initialDevice.subscriptions = Collections.singletonList(subscription);
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        initialDevice.appKey = null;
        initialDevice.userKey = null;

        // Test saving the device.
        assertTrue(mDeviceDao.saveDevice(initialDevice));

        // Test retrieving the same device.
        Device fetchedDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertNotNull(fetchedDevice.account);
        assertEquals(fetchedDevice.subscriptions.size(), 1);
        assertNotNull(fetchedDevice.subscriptions.get(0).route);
    }

    @Test
    public void testDatabaseSubscriptionUpdate() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        Route initialRoute = new Route("test_route_1");
        Subscription initialSubscription = new Subscription();
        initialSubscription.route = initialRoute;
        initialSubscription.device = initialDevice;

        initialDevice.subscriptions = Collections.singletonList(initialSubscription);

        // Test saving the device.
        assertTrue(mDeviceDao.saveDevice(initialDevice));

        // Test retrieving the same device.
        Device fetchedDevice = mDeviceDao.getDevice(initialDevice.deviceId);

        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertNotNull(fetchedDevice.account);
        assertEquals(fetchedDevice.subscriptions.size(), 1);

        // Save some more routes.
        Route updatedRoute1 = new Route("test_route_1");
        Route updatedRoute2 = new Route("test_route_2");
        Route updatedRoute3 = new Route("test_route_3");
        Agency updatedAgency = TestModelHelper.createTestAgency();
        updatedAgency.routes = Arrays.asList(updatedRoute1, updatedRoute2, updatedRoute3);
        mAgencyDao.saveAgency(updatedAgency);

        Device updatedDevice = TestModelHelper.createTestDevice();
        updatedDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        Subscription subscription1 = new Subscription();
        Subscription subscription2 = new Subscription();
        Subscription subscription3 = new Subscription();

        subscription1.route = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, "test_route_1");
        subscription2.route = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, "test_route_2");
        subscription3.route = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, "test_route_3");

        updatedDevice.subscriptions = Arrays.asList(subscription1, subscription2, subscription3);
        updatedDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        // Test saving the updated device.
        assertTrue(mDeviceDao.saveDevice(updatedDevice));

        fetchedDevice = mDeviceDao.getDevice(TestModelHelper.TEST_DEVICE_ID);
        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertNotNull(fetchedDevice.account);
        assertEquals(fetchedDevice.subscriptions.size(), 3);
        assertNotNull(fetchedDevice.subscriptions.get(0).route);
        assertNotNull(fetchedDevice.subscriptions.get(1).route);
        assertNotNull(fetchedDevice.subscriptions.get(2).route);
    }

    @Test
    public void testDatabaseSubscriptionRemove() {
        Device initialDevice = TestModelHelper.createTestDevice();
        initialDevice.subscriptions = null;
        initialDevice.account = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);

        assertTrue(mDeviceDao.saveDevice(initialDevice));

        Device fetchedDevice = mDeviceDao.getDevice(initialDevice.deviceId);

        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertTrue(fetchedDevice.subscriptions.isEmpty());
    }
}
