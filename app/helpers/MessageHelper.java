package helpers;

import enums.AlertLevel;
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

    private enum MessageRootKey {
        MESSAGE_TYPE("alert_type"),
        MESSAGE("message");

        private String value;

        MessageRootKey(String value) {
            this.value = value;
        }
    }

    // GCM keys for the type of message being sent
    private enum MessageType {
        REGISTERED_ON_NETWORK("registered_on_network"),
        RESEND_SUBSCRIPTIONS("resend_subscriptions"),
        ALERT_CURRENT_MESSAGE("current_alert"),
        ALERT_DETOUR_MESSAGE("detour_alert"),
        ALERT_ADVISORY_MESSAGE("advisory_alert"),
        ALERT_APP_MESSAGE("app_alert"),
        ALERT_CANCEL("cancel_alert");

        public String value;

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
            gcmMessage.shouldDelayWhileIdle = true;
            gcmMessage.addRegistrationToken(registration.registrationToken);
            gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.REGISTERED_ON_NETWORK.value);
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
    public static Message buildPushMessage(@Nonnull Alert updatedAlert, boolean isCancelMessage,
                                           @Nonnull List<Registration> registrations,
                                           @Nonnull List<PlatformAccount> platformAccounts) {

        Message gcmMessage = addMessagePlatformAccounts(new Message(), platformAccounts);
        if (gcmMessage != null) {

            gcmMessage.shouldDelayWhileIdle = true;
            gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_ID.value, updatedAlert.route.routeId);
            gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_NAME.value, updatedAlert.route.routeName);

            for (Registration registration : registrations) {
                gcmMessage.addRegistrationToken(registration.registrationToken);
            }

            // Add the main message.
            gcmMessage.addData(MessageRootKey.MESSAGE.value, updatedAlert.messageBody);

            // Default push service priorities based on what the deserializer assigned.
            AlertLevel alertLevel = updatedAlert.level;

            if (alertLevel.equals(AlertLevel.LEVEL_MEDIUM)) {
                gcmMessage.priority = Message.Priority.PRIORITY_NORMAL;

            } else if (alertLevel.equals(AlertLevel.LEVEL_HIGH)) {
                gcmMessage.priority = Message.Priority.PRIORITY_HIGH;

            } else if (alertLevel.equals(AlertLevel.LEVEL_CRITICAL)) {
                gcmMessage.priority = Message.Priority.PRIORITY_HIGH;

            } else {
                gcmMessage.priority = Message.Priority.PRIORITY_LOW;
            }

            // First ascertain whether the alert should be flagged as a cancel message.
            if (isCancelMessage) {
                gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_CANCEL.value);
                gcmMessage.collapseKey = updatedAlert.route.routeId;

            } else {
                switch (updatedAlert.type) {
                    // Switch between different GCM message types depending on the alert.

                    // Detour GCM message.
                    case TYPE_DETOUR:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_DETOUR_MESSAGE.value);
                        gcmMessage.collapseKey = updatedAlert.route.routeId;
                        break;

                    // Detour GCM message.
                    case TYPE_INFORMATION:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_ADVISORY_MESSAGE.value);
                        gcmMessage.collapseKey = updatedAlert.route.routeId;
                        gcmMessage.priority = Message.Priority.PRIORITY_LOW;
                        break;

                    // Disruption message.
                    case TYPE_DISRUPTION:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = updatedAlert.route.routeId;
                        break;

                    // High priority Weather GCM message.
                    case TYPE_WEATHER:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = MessageType.ALERT_CURRENT_MESSAGE.value;
                        gcmMessage.priority = Message.Priority.PRIORITY_HIGH;
                        break;

                    // High priority Weather GCM message.
                    case TYPE_APP:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_APP_MESSAGE.value);
                        gcmMessage.collapseKey = MessageType.ALERT_APP_MESSAGE.value;
                        break;

                    case TYPE_MAINTENANCE:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, MessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = updatedAlert.route.routeId;
                }
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
    @Nonnull
    public static Message cloneMessage(@Nonnull Message message) {
        Message clonedMessage = new Message();
        clonedMessage.id = message.id;
        clonedMessage.authToken = message.authToken;
        clonedMessage.endpointUrl = message.endpointUrl;
        clonedMessage.recipients = new ArrayList<>();
        clonedMessage.payloadData = message.payloadData;
        clonedMessage.collapseKey = message.collapseKey;
        clonedMessage.ttl = message.ttl;
        clonedMessage.restrictedPackageName = message.restrictedPackageName;
        clonedMessage.isDryRun = message.isDryRun;
        clonedMessage.shouldDelayWhileIdle = message.shouldDelayWhileIdle;
        clonedMessage.priority = message.priority;
        clonedMessage.task = message.task;
        clonedMessage.sentTime = message.sentTime;

        return clonedMessage;
    }
}
