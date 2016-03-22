package helpers;

import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.registrations.Registration;
import models.taskqueue.Message;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Used to easily create relevent push platform messages which can be sent
 * out using the dispatchers.
 */
public class GcmMessageHelper {
    private static final String TAG = GcmMessageHelper.class.getSimpleName();

    private enum MessageRoot {
        MESSAGE_TYPE("alert_type"),
        MESSAGE("message");

        private String value;
        MessageRoot(String value) {
            this.value = value;
        }
    }

    private enum MessageType {
        REGISTERED_ON_NETWORK("registered_on_network"),
        RESEND_SUBSCRIPTIONS("resend_subscriptions"),
        ALERT_CURRENT_MESSAGE("current_alert"),
        ALERT_DETOUR_MESSAGE("detour_alert"),
        ALERT_ADVISORY_MESSAGE("advisory_alert"),
        ALERT_APP_MESSAGE("app_alert"),
        ALERT_CANCEL("cancel_alert");

        private String value;
        MessageType(String value) {
            this.value = value;
        }
    }

    private enum AlertMessage {
        GCM_KEY_ROUTE_ID("route_id"),
        GCM_KEY_ROUTE_NAME("route_name");

        private String value;
        AlertMessage(String value) {
            this.value = value;
        }
    }

    /**
     * Build a GCM device confirmation push message.
     *
     * @param registration    registration of device to send to.
     * @param platformAccount account holder of registrationId
     * @return Message.
     */
    public static Message buildConfirmDeviceMessage(@Nonnull Registration registration,
                                                    @Nonnull PlatformAccount platformAccount) {
        final int ONE_WEEK_IN_SECONDS = 60 * 60 * 24 * 7;

        Message gcmMessage = new Message();
        gcmMessage.platformAccount = platformAccount;
        gcmMessage.collapseKey = MessageType.REGISTERED_ON_NETWORK.value;
        gcmMessage.ttl = ONE_WEEK_IN_SECONDS;
        gcmMessage.isDelayWhileIdle = true;
        gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.REGISTERED_ON_NETWORK.value);
        return gcmMessage;
    }

    /**
     * Add an alert message (detour, advisory, etc) and a set of
     * registrations to the preprocessor.
     *
     * @param updatedAlert    alert which has been validated as update ready..
     * @param platformAccount the gcm account to send the message from.
     */
    public static Message buildAlertMessage(@Nonnull Alert updatedAlert,
                                            @Nonnull List<Registration> registrations,
                                            @Nonnull PlatformAccount platformAccount) {
        Message gcmMessage = new Message();
        gcmMessage.platformAccount = platformAccount;
        gcmMessage.isDelayWhileIdle = true;

        gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_ID.value, updatedAlert.route.routeId);
        gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_NAME.value, updatedAlert.route.routeName);

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
            gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.ALERT_CANCEL.value);

        } else if (!datesEmpty || !CompareUtils.isEmptyNullSafe(
                updatedAlert.detourMessage,
                updatedAlert.detourStartLocation,
                updatedAlert.detourReason)) {
            // Detour alert if there is some detour information.
            gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.ALERT_DETOUR_MESSAGE.value);
            gcmMessage.addData(MessageRoot.MESSAGE.value, updatedAlert.detourMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.currentMessage)) {
            // Current Message has updated.
            gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.ALERT_CURRENT_MESSAGE.value);
            gcmMessage.addData(MessageRoot.MESSAGE.value, updatedAlert.currentMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.advisoryMessage)) {
            // Advisory Message has updated.
            gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.ALERT_ADVISORY_MESSAGE.value);
            gcmMessage.addData(MessageRoot.MESSAGE.value, updatedAlert.advisoryMessage);
            gcmMessage.collapseKey = updatedAlert.route.routeId;
        }
        return gcmMessage;
    }
}
