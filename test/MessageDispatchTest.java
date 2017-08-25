import dao.AccountDao;
import dao.AgencyDao;
import dao.DeviceDao;
import javafx.util.Pair;
import models.AlertModifications;
import models.accounts.Account;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import models.pushservices.db.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import services.PushMessageManager;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test core functions of the Message Dispatching system.
 */
public class MessageDispatchTest extends CommuteTestApplication {
    private static PushMessageManager mPushMessageManager;
    private static AccountDao mAccountDao;
    private static DeviceDao mDeviceDao;
    private static AgencyDao mAgencyDao;

    private static Account mAccount;
    private static Device mDevice;
    private static Agency mAgency;
    private static Route mRoute;

    @BeforeClass
    public static void initialise() {
        mPushMessageManager = application.injector().instanceOf(PushMessageManager.class);
        mDeviceDao = application.injector().instanceOf(DeviceDao.class);
        mAgencyDao = application.injector().instanceOf(AgencyDao.class);
        mAccountDao = application.injector().instanceOf(AccountDao.class);

        mAgency = TestModelHelper.createTestAgency();
        mAccount = TestModelHelper.createTestAccount();
        mDevice = TestModelHelper.createTestDevice();
        mDevice.account = mAccount;

        mAccountDao.saveAccount(mAccount);
        mAgencyDao.saveAgency(mAgency);
        mRoute = mAgencyDao.getRoute(TestModelHelper.AGENCY_ID, TestModelHelper.ROUTE_ID);

        Subscription subscription = new Subscription();
        subscription.route = mRoute;
        subscription.device = mDevice;
        mDevice.subscriptions = Collections.singletonList(subscription);

        assertTrue(mDeviceDao.saveDevice(mDevice));
    }

    @AfterClass
    public static void shutdown() {
        mDeviceDao.removeDevice(mDevice.token);
        mAgencyDao.removeAgency(mAgency.id);
        mAccountDao.removeAccount(mAccount.id);
    }

    @Test
    public void testNoMessageDispatch() {
        AlertModifications alertModifications = new AlertModifications(TestModelHelper.AGENCY_ID);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertTrue(dispatchedMessages.getKey().isEmpty());
        assertTrue(dispatchedMessages.getValue().isEmpty());
    }

    @Test
    public void testRouteAlertUpdateDispatch() {
        Alert updatedAlert = TestModelHelper.createTestAlert();
        Route updatedRoute = TestModelHelper.createTestRoute();
        updatedRoute.alerts = Collections.singletonList(updatedAlert);
        updatedAlert.route = updatedRoute;

        AlertModifications alertModifications = new AlertModifications(TestModelHelper.AGENCY_ID);
        alertModifications.addUpdatedAlert(updatedAlert);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertFalse(dispatchedMessages.getKey().isEmpty());
        assertTrue(dispatchedMessages.getValue().isEmpty());
    }

    @Test
    public void testRouteAlertStaleDispatch() {
        Alert staleAlert = TestModelHelper.createTestAlert();
        Route staleRoute = TestModelHelper.createTestRoute();
        staleRoute.alerts = Collections.singletonList(staleAlert);
        staleAlert.route = staleRoute;

        AlertModifications alertModifications = new AlertModifications(TestModelHelper.AGENCY_ID);
        alertModifications.addStaleAlert(staleAlert);

        Pair<Set<Message>, Set<Message>> dispatchedMessages = mPushMessageManager.dispatchAlerts(alertModifications);
        assertTrue(dispatchedMessages.getKey().isEmpty());
        assertFalse(dispatchedMessages.getValue().isEmpty());
        assertEquals(dispatchedMessages.getValue().size(), 1);
    }

    @Test
    public void testDeviceRegistrationDispatch() {
        assertNotNull(mAccount.platformAccounts);
        assertFalse(mAccount.platformAccounts.isEmpty());
        assertTrue(mPushMessageManager.sendRegistrationConfirmMessage(mDevice, mAccount.platformAccounts.get(0)));
    }
}
