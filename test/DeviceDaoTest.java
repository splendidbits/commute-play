import daos.DeviceDao;
import models.accounts.Account;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class DeviceDaoTest extends CommuteTestApplication {
    private static String TEST_DEVICE_ID = "test_id_123456";
    private static String TEST_DEVICE_TOKEN = "test_token_123456";

    private static DeviceDao mDeviceDao;
    private Account mAccount;

    @BeforeClass
    public static void initialise() {
        mDeviceDao = application.injector().instanceOf(DeviceDao.class);
    }

    @Before
    public void startup() {
        mDeviceDao.saveDevice(new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN));

        mAccount = Mockito.mock(Account.class);
        Mockito.when(mAccount.active).thenReturn(true);
        Mockito.when(mAccount.apiKey).thenReturn("apiKey");
    }

    @After
    public void teardown() {
        mDeviceDao.removeDevice(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
    }

    @Test
    public void testSaveDevice() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.timeRegistered = new Date();
        device.account = mAccount;

        assertTrue(mDeviceDao.saveDevice(device));
    }

    @Test
    public void testChangeDeviceToken() {
        // Change device token to new token id.
        String newDeviceToken = "updated_test_token_123456";
        Device staleDevice = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        Device updatedDevice = new Device(TEST_DEVICE_ID, newDeviceToken);
        staleDevice.account = mAccount;
        updatedDevice.account = mAccount;

        assertTrue(mDeviceDao.saveUpdatedToken(staleDevice.token, updatedDevice.token));

        // Get the changed device.
        Device fetchedDevice = mDeviceDao.getDevice(updatedDevice.deviceId);
        assertNotNull(fetchedDevice);
        assertEquals(fetchedDevice.token, newDeviceToken);

        // Change it back
        assertTrue(mDeviceDao.saveUpdatedToken(newDeviceToken, TEST_DEVICE_TOKEN));
    }

    @Test
    public void testRemoveDevice() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.account = mAccount;

        // Add test device.
        mDeviceDao.saveDevice(device);

        // Remove device.
        assertTrue(mDeviceDao.removeDevice(device.deviceId, device.token));
    }

    @Test
    public void testSaveSubscriptions() {
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

        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.subscriptions = subscriptions;
        device.account = mAccount;

        // Test saving the device.
        assertTrue(mDeviceDao.saveDevice(device));

        // Test retrieving the same device.
        Device fetchedDevice = mDeviceDao.getDevice(TEST_DEVICE_ID);
        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertNotNull(fetchedDevice.account);
        assertEquals(fetchedDevice.subscriptions.size(), 3);
    }

    @Test
    public void testRemoveSubscriptions() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.subscriptions = null;
        device.account = mAccount;

        assertTrue(mDeviceDao.saveDevice(device));

        Device fetchedDevice = mDeviceDao.getDevice(TEST_DEVICE_ID);

        assertNotNull(fetchedDevice);
        assertNotNull(fetchedDevice.subscriptions);
        assertTrue(fetchedDevice.subscriptions.isEmpty());
    }
}
