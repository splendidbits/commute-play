package helpers;

import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.registrations.Registration;
import models.taskqueue.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to easily create relevent push platform messages which can be sent
 * out using the dispatchers.
 */
public class MessageHelper {
    private static final String TAG = MessageHelper.class.getSimpleName();

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
     * @param registration     registration of device to send to.
     * @param platformAccounts account holders of registration
     * @return Message.
     */
    @Nullable
    public static Message buildConfirmDeviceMessage(@Nonnull Registration registration,
                                                    @Nonnull List<PlatformAccount> platformAccounts) {
        final int ONE_WEEK_IN_SECONDS = 60 * 60 * 24 * 7;

        Message gcmMessage = addMessagePlatformAccounts(new Message(), platformAccounts);
        if (gcmMessage != null) {

            gcmMessage.collapseKey = MessageType.REGISTERED_ON_NETWORK.value;
            gcmMessage.ttl = ONE_WEEK_IN_SECONDS;
            gcmMessage.isDelayWhileIdle = true;
            gcmMessage.addRegistrationToken(registration.registrationToken);
            gcmMessage.addData(MessageRoot.MESSAGE_TYPE.value, MessageType.REGISTERED_ON_NETWORK.value);
        }
        return gcmMessage;
    }

    /**
     * Add an alert message (detour, advisory, etc) and a set of
     * registrations to the preprocessor.
     *
     * @param updatedAlert     alert which has been validated as update ready..
     * @param platformAccounts account holders for alert.
     */
    @Nullable
    public static Message buildAlertMessage(@Nonnull Alert updatedAlert,
                                            @Nonnull List<Registration> registrations,
                                            @Nonnull List<PlatformAccount> platformAccounts) {

        Message gcmMessage = addMessagePlatformAccounts(new Message(), platformAccounts);
        if (gcmMessage != null) {

            gcmMessage.isDelayWhileIdle = true;
            gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_ID.value, updatedAlert.route.routeId);
            gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_NAME.value, updatedAlert.route.routeName);

            for (Registration registration : registrations) {
                gcmMessage.addRegistrationToken(registration.registrationToken);
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
                // If all of the above in the alert was empty, send a cancel alert logMessage.
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
        }
        return gcmMessage;
    }

    /**
     * Add platform accounts to a message.
     *
     * @param message          message to add platform accounts to
     * @param platformAccounts list of accounts for message.
     * @return Message populated with accounts, or null if no accounts.
     */
    private static Message addMessagePlatformAccounts(@Nonnull Message message,
                                                      @Nonnull List<PlatformAccount> platformAccounts) {
        // Add accounts to the message.
        for (PlatformAccount account : platformAccounts) {
            if (account.platform != null && account.platform.endpointUrl != null) {
                message.endpointUrl = account.platform.endpointUrl;
                message.authToken = account.authToken;
            }
        }

        // If there are no platform accounts, return null.
        if (message.authToken == null || message.endpointUrl == null) {
            return null;
        }
        return message;
    }

    /**
     * Clone a message completely without any registration information
     *
     * @param message message to copy.
     * @return a copied message that is exactly the same but with registration information removed.
     */
    public static Message cloneMessage(Message message) {
        if (message != null) {
            Message clonedMessage = new Message();
            clonedMessage.messageId = message.messageId;
            clonedMessage.authToken = message.authToken;
            clonedMessage.endpointUrl = message.endpointUrl;
            clonedMessage.recipients = new ArrayList<>();
            clonedMessage.payloadData = message.payloadData;
            clonedMessage.collapseKey = message.collapseKey;
            clonedMessage.ttl = message.ttl;
            clonedMessage.restrictedPackageName = message.restrictedPackageName;
            clonedMessage.isDryRun = message.isDryRun;
            clonedMessage.isDelayWhileIdle = message.isDelayWhileIdle;
            clonedMessage.priority = message.priority;
            clonedMessage.task = message.task;
            clonedMessage.sentTime = message.sentTime;

            return clonedMessage;
        }
        return null;
    }
}
