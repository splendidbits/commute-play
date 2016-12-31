package dao;

import enums.pushservices.PlatformType;
import main.AbstractApplicationTest;
import models.accounts.Account;
import models.alerts.Agency;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AccountDaoTest extends AbstractApplicationTest {
    private static DeviceDao mDeviceDao;
    private static AccountDao mAccountDao;
    private static AgencyDao mAgencyDao;

    @BeforeClass
    public static void initialise() {
        mDeviceDao = application.injector().instanceOf(DeviceDao.class);
        mAccountDao = application.injector().instanceOf(AccountDao.class);
        mAgencyDao = application.injector().instanceOf(AgencyDao.class);
    }

    @Before
    public void startup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void testAccountDevice() {
        String deviceId = "test_id_123456";
        String deviceToken = "test_token_123456";

        // Get an existing agency.
        Agency agency = mAgencyDao.getAgency(2);
        assertNotNull(agency);
        assertNotNull(agency.routes);
        assertFalse(agency.routes.isEmpty());

        // Get a valid account
        Account mainAccount = mAccountDao.getAccountForKey("UfhV6Lt");
        assertNotNull(mainAccount);

        // Set up a new device with a route subscription for the agency.
        Route route = agency.routes.get(0);
        Subscription subscription = new Subscription();
        subscription.route = agency.routes.get(0);

        Device device = new Device(deviceId, deviceToken);
        device.timeRegistered = new Date();
        device.account = mainAccount;
        device.subscriptions = Collections.singletonList(subscription);

        // Save a new device with subscription for a route/
        assertTrue(mDeviceDao.saveDevice(device));

        // Fetch the persisted device.
        List<Account> accounts = mAccountDao.getAccountDevices(PlatformType.SERVICE_GCM, agency.id, route.routeId);
        Device matchingDevice = null;

        for (Account account : accounts) {
            if (matchingDevice != null) {
                break;
            }

            List<Device> accountDevices = account.devices;
            assertNotNull(accountDevices);

            for (Device accountDevice : accountDevices) {
                if (accountDevice.deviceId.equals(device.deviceId)) {
                    matchingDevice = accountDevice;
                    break;
                }
            }
        }

        assertNotNull(matchingDevice);
        assertEquals(matchingDevice.token, device.token);

        // Remove the device
        assertTrue(mDeviceDao.removeDevice(deviceToken));
    }
}
