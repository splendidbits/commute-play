import dao.AccountDao;
import dao.DeviceDao;
import models.accounts.Account;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.junit.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class DeviceTest extends CommuteTestApplication {
    private static String TEST_DEVICE_ID = "test_id_123456";
    private static String TEST_DEVICE_TOKEN = "test_token_123456";
    private static String ACCOUNT_API_KEY  = "UfhV6Lt";

    private static DeviceDao mDeviceDao;
    private static Account mAccount;

    @BeforeClass
    public static void initialise() {
        mDeviceDao = application.injector().instanceOf(DeviceDao.class);

        AccountDao mAccountDao = application.injector().instanceOf(AccountDao.class);
        mAccount = mAccountDao.getAccountForKey(ACCOUNT_API_KEY);
    }

    @Before
    public void beforeTest() {

    }

    @After
    public void afterTest() {
        mDeviceDao.removeDevice(TEST_DEVICE_TOKEN);
    }

    @Test
    public void testDatabaseDeviceInsert() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.timeRegistered = new Date();
        device.account = mAccount;

        assertTrue(mDeviceDao.saveDevice(device));
        assertTrue(mDeviceDao.removeDevice(device.token));
    }

    @Test
    public void testDatabaseDeviceUpdate() {
        assertTrue(mDeviceDao.saveDevice(new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN)));

        Device existingDevice = mDeviceDao.getDevice(TEST_DEVICE_ID);
        assertNotNull(existingDevice);

        Date currentTime = new Date();
        Device newDevice = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        newDevice.timeRegistered = currentTime;
        newDevice.account = existingDevice.account;

        assertTrue(mDeviceDao.saveDevice(newDevice));
        Device savedDevice = mDeviceDao.getDevice(TEST_DEVICE_ID);
        assertNotNull(savedDevice);
    }

    @Test
    public void testDatabaseDeviceTokenUpdate() {
        // Change device token to new token id.
        String newDeviceToken = "updated_test_token_123456";
        Device staleDevice = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        mDeviceDao.saveDevice(staleDevice);

        Device updatedDevice = new Device(TEST_DEVICE_ID, newDeviceToken);
        assertTrue(mDeviceDao.saveUpdatedToken(staleDevice.token, updatedDevice.token));

        // Get the changed device.
        Device fetchedDevice = mDeviceDao.getDevice(updatedDevice.deviceId);
        assertNotNull(fetchedDevice);
        assertEquals(fetchedDevice.token, newDeviceToken);

        // Change it back
        assertTrue(mDeviceDao.saveUpdatedToken(newDeviceToken, TEST_DEVICE_TOKEN));
    }

    @Test
    public void testDatabaseDeviceRemove() {
        Device device = new Device(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
        device.account = mAccount;

        // Add test device.
        mDeviceDao.saveDevice(device);

        // Remove device.
        assertTrue(mDeviceDao.removeDevice(device.token));
        assertNull(mDeviceDao.getDevice(TEST_DEVICE_ID));
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
    public void testDatabaseSubscriptionRemove() {
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
