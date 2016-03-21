package services.gcm;

import helpers.CompareUtils;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import models.taskqueue.Message;
import services.AccountServiceDao;

import javax.annotation.Nonnull;
import java.util.List;

import static models.accounts.Platform.PLATFORM_NAME_GCM;

/**
 * Sets up things for the GCM Dispatcher.
 */
public class GcmAlertProcessor {
    private static final String GCM_KEY_ROUTE_ID = "route_id";
    private static final String GCM_KEY_ALERT_TYPE = "alert_type";
    private static final String GCM_KEY_ROUTE_NAME = "route_name";
    private static final String GCM_KEY_MESSAGE = "message";
    private static final String TAG = GcmAlertProcessor.class.getSimpleName();

    private enum AlertUpdateType {
        ALERT_CURRENT_MESSAGE("current_alert"),
        ALERT_DETOUR_MESSAGE("detour_alert"),
        ALERT_ADVISORY_MESSAGE("advisory_alert"),
        ALERT_APP_MESSAGE("app_alert"),
        ALERT_CANCEL("cancel_alert");

        private String value;

        AlertUpdateType(String value) {
            this.value = value;
        }
    }

    /**
     * TODO: Migrate this to a polling TaskQueue service.
     *
     * Notify GCM subscribers of the modified alerts which have changed.
     *
     * @param modifiedAlerts Collection of modified alerts including their routes.
     */
    public void notifyAlertSubscribers(@Nonnull ModifiedAlerts modifiedAlerts) {
        List<Alert> updatedAlerts = modifiedAlerts.getUpdatedAlerts();

        // Iterate through the routes.
        for (Alert alert : updatedAlerts) {

            // Get all accounts for the registrations subscribed to that route.
            AccountServiceDao accountServiceDao = new AccountServiceDao();
            List<Account> accounts = accountServiceDao.getRegistrationAccounts(
                    PLATFORM_NAME_GCM,
                    modifiedAlerts.getAgencyId(),
                    alert.route);

            if (accounts != null && !accounts.isEmpty()) {

                // Loop through each sending API account.
                for (Account account : accounts) {

                    for (PlatformAccount platformAccount : account.platformAccounts) {
                        Message message = getGoogleMessage(alert, account.registrations, platformAccount);
                        new GoogleGcmDispatcher(message, new PushResponseReceiver());
                    }
                }
            }
        }
    }

    /**
     * Add an alert message (detour, advisory, etc) and a set of
     * registrations to the preprocessor.
     *
     * @param updatedAlert  alert which has been validated as update ready..
     * @param platformAccount the gcm account to send the message from.
     */
    private Message getGoogleMessage(@Nonnull Alert updatedAlert,
                                     @Nonnull List<Registration> registrations,
                                     @Nonnull PlatformAccount platformAccount) {
        Message gcmMessage = new Message();
        gcmMessage.account = platformAccount;

        gcmMessage.addData(GCM_KEY_ROUTE_ID, updatedAlert.route.routeId);
        gcmMessage.addData(GCM_KEY_ROUTE_NAME, updatedAlert.route.routeName);

        for (Registration registration : registrations) {
            gcmMessage.addRegistrationId(registration.registrationToken);
        }

        boolean messageDataEmpty = CompareUtils.isEmptyNullSafe(
                updatedAlert.currentMessage,
                updatedAlert.advisoryMessage,
                updatedAlert.detourMessage,
                updatedAlert.detourStartLocation,
                updatedAlert.detourReason);

        boolean datesEmpty = updatedAlert.detourStartDate == null
                && updatedAlert.detourEndDate == null;

        boolean noSnow = !updatedAlert.isSnow;

        if (messageDataEmpty && datesEmpty && noSnow) {
            // If all of the above in the alert was empty, send a cancel alert message.
            gcmMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_CANCEL.value);

        } else if (!datesEmpty || !CompareUtils.isEmptyNullSafe(
                updatedAlert.detourMessage,
                updatedAlert.detourStartLocation,
                updatedAlert.detourReason)) {
            // Detour alert if there is some detour information.
            gcmMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_DETOUR_MESSAGE.value);
            gcmMessage.addData(GCM_KEY_MESSAGE, updatedAlert.detourMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.currentMessage)) {
            // Current Message has updated.
            gcmMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_CURRENT_MESSAGE.value);
            gcmMessage.addData(GCM_KEY_MESSAGE, updatedAlert.currentMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.advisoryMessage)) {
            // Advisory Message has updated.
            gcmMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_ADVISORY_MESSAGE.value);
            gcmMessage.addData(GCM_KEY_MESSAGE, updatedAlert.advisoryMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;
        }
        return gcmMessage;
    }
}
