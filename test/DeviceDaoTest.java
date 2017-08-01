import dao.AccountDao;
import dao.DeviceDao;
import models.accounts.Account;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class DeviceDaoTest extends CommuteTestApplication {
    private static DeviceDao mDeviceDao;
    private static Account mAccount;
    private static AccountDao mAccountDao;

    private static Device initialDevice;

    @BeforeClass
    public static void initialise() {
        mDeviceDao = application.injector().instanceOf(DeviceDao.class);
        mAccountDao = application.injector().instanceOf(AccountDao.class);

        // Save an account
        Account newAccount = TestModelHelper.createTestAccount();
        mAccountDao.saveAccount(newAccount);

        mAccount = mAccountDao.getAccountForKey(TestModelHelper.ACCOUNT_API_KEY);
        initialDevice = TestModelHelper.createTestDevice();
    }

    @AfterClass
    public static void shutdown() {
        mAccountDao.removeAccount(mAccount.id);
    }


    @After
    public void afterTest() {
        assertTrue(mDeviceDao.removeDevice(initialDevice.token));
    }

    @Test
    public void testDatabaseDeviceInsert() {
        initialDevice.account = mAccount;

        assertTrue(mDeviceDao.saveDevice(initialDevice));
        assertTrue(mDeviceDao.removeDevice(initialDevice.token));
    }

    @Test
    public void testDatabaseDeviceUpdate() {
        assertTrue(mDeviceDao.saveDevice(initialDevice));

        Device existingDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(existingDevice);

        Date currentTime = new Date();
        Device newDevice = TestModelHelper.createTestDevice();
        newDevice.timeRegistered = currentTime;
        newDevice.account = existingDevice.account;

        assertTrue(mDeviceDao.saveDevice(newDevice));
        Device savedDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(savedDevice);
    }

    @Test
    public void testDatabaseDeviceTokenUpdate() {
        // Change device token to new token id.
        String newDeviceToken = "updated_test_token_123456";
        mDeviceDao.saveDevice(initialDevice);

        Device updatedDevice = new Device(initialDevice.deviceId, newDeviceToken);
        assertTrue(mDeviceDao.saveUpdatedToken(initialDevice.token, updatedDevice.token));

        // Get the changed device.
        Device fetchedDevice = mDeviceDao.getDevice(updatedDevice.deviceId);
        assertNotNull(fetchedDevice);
        assertEquals(fetchedDevice.token, newDeviceToken);

        // Change it back
        assertTrue(mDeviceDao.saveUpdatedToken(newDeviceToken, initialDevice.token));
    }

    @Test
    public void testDatabaseDeviceRemove() {
        initialDevice.account = mAccount;

        // Add test device.
        mDeviceDao.saveDevice(initialDevice);

        // Remove device.
        assertTrue(mDeviceDao.removeDevice(initialDevice.token));
        assertNull(mDeviceDao.getDevice(initialDevice.deviceId));
    }

    @Test
    public void testDatabaseSubscriptionSave() {
        List<Subscription> subscriptions = new ArrayList<>();
        Subscription route1 = new Subscription();
        Subscription route2 = new Subscription();
        Subscription route3 = new Subscription();

        route1.route = new Route("test_route_1");
        route2.route = new Route("test_route_2");
        route3.route = new Route("test_route_3");

        subscriptions.add(route1);
        subscriptions.add(route2);
        subscriptions.add(route3);

        initialDevice.subscriptions = subscriptions;
        initialDevice.account = mAccount;

        // Test saving the device.
        assertTrue(mDeviceDao.saveDevice(initialDevice));

        // Test retrieving the same device.
        Device fetchedDevice = mDeviceDao.getDevice(initialDevice.deviceId);
        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertNotNull(fetchedDevice.account);
        assertEquals(fetchedDevice.subscriptions.size(), 3);
    }

    @Test
    public void testDatabaseSubscriptionRemove() {
        initialDevice.subscriptions = null;
        initialDevice.account = mAccount;

        assertTrue(mDeviceDao.saveDevice(initialDevice));

        Device fetchedDevice = mDeviceDao.getDevice(initialDevice.deviceId);

        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertTrue(fetchedDevice.subscriptions.isEmpty());
    }
}
