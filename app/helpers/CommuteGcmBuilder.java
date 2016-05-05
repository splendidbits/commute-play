package helpers;

import enums.AlertLevel;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.devices.Device;
import pushservices.enums.MessagePriority;
import pushservices.models.database.Credentials;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to easily create relevent push platform messages which can be sent
 * out using the dispatchers.
 */
public class CommuteGcmBuilder {
    private static final String TAG = CommuteGcmBuilder.class.getSimpleName();

    private enum MessageRootKey {
        MESSAGE_TYPE("alert_type"),
        MESSAGE("message");

        private String value;

        MessageRootKey(String value) {
            this.value = value;
        }
    }

    // GCM keys for the type of message being sent
    private enum CommuteMessageType {
        REGISTERED_ON_NETWORK("registered_on_network"),
        RESEND_SUBSCRIPTIONS("resend_subscriptions"),
        ALERT_CURRENT_MESSAGE("current_message"),
        ALERT_DETOUR_MESSAGE("detour_message"),
        ALERT_ADVISORY_MESSAGE("advisory_message"),
        ALERT_APP_MESSAGE("app_message"),
        ALERT_CANCEL("cancel_message");

        public String value;

        CommuteMessageType(String value) {
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
     * @param device           device to send to.
     * @param platformAccounts push service platform accounts.
     * @return Message.
     */
    @Nullable
    public static Message buildConfirmDeviceMessage(@Nonnull Device device,
                                                    @Nonnull List<PlatformAccount> platformAccounts) {
        final int ONE_WEEK_IN_SECONDS = 60 * 60 * 24 * 7;

        Message gcmMessage = addMessagePlatformAccounts(new Message(), platformAccounts);
        if (gcmMessage != null && device.token != null) {
            gcmMessage.collapseKey = CommuteMessageType.REGISTERED_ON_NETWORK.value;
            gcmMessage.ttl = ONE_WEEK_IN_SECONDS;
            gcmMessage.shouldDelayWhileIdle = true;
            gcmMessage.addRecipient(new Recipient(device.token));
            gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.REGISTERED_ON_NETWORK.value);
        }
        return gcmMessage;
    }

    /**
     * Add an alert message (detour, advisory, etc) and a set of devices to the preprocessor.
     *
     * @param alert     alert which has been validated as update ready..
     * @param platformAccounts account holders for agency alert.
     */
    @Nullable
    public static Message buildPushMessage(@Nonnull Alert alert, boolean isCancelMessage,
                                           @Nonnull List<Device> devices,
                                           @Nonnull List<PlatformAccount> platformAccounts) {

        Message gcmMessage = addMessagePlatformAccounts(new Message(), platformAccounts);
        if (gcmMessage != null && alert.route != null && alert.route.routeId != null) {

            gcmMessage.shouldDelayWhileIdle = true;
            gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_ID.value, alert.route.routeId);

            if (alert.route.routeName != null) {
                gcmMessage.addData(AlertMessage.GCM_KEY_ROUTE_NAME.value, alert.route.routeName);
            }
            if (alert.messageBody != null) {
                gcmMessage.addData(MessageRootKey.MESSAGE.value, alert.messageBody);
            }
            for (Device device : devices) {
                gcmMessage.addRecipient(new Recipient(device.token));
            }

            // Default push service priorities based on what the deserializer assigned.
            AlertLevel alertLevel = alert.level;

            if (alertLevel.equals(AlertLevel.LEVEL_NORMAL)) {
                gcmMessage.messagePriority = MessagePriority.PRIORITY_NORMAL;

            } else if (alertLevel.equals(AlertLevel.LEVEL_HIGH)) {
                gcmMessage.messagePriority = MessagePriority.PRIORITY_HIGH;

            } else if (alertLevel.equals(AlertLevel.LEVEL_CRITICAL)) {
                gcmMessage.messagePriority = MessagePriority.PRIORITY_HIGH;

            } else {
                gcmMessage.messagePriority = MessagePriority.PRIORITY_LOW;
            }

            // First ascertain whether the alert should be flagged as a cancel message.
            if (isCancelMessage) {
                gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_CANCEL.value);
                gcmMessage.collapseKey = alert.route.routeId;

            } else {
                switch (alert.type) {

                    // Detour GCM message.
                    case TYPE_DETOUR:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_DETOUR_MESSAGE.value);
                        gcmMessage.collapseKey = alert.route.routeId;
                        break;

                    // Detour GCM message.
                    case TYPE_INFORMATION:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_ADVISORY_MESSAGE.value);
                        gcmMessage.collapseKey = alert.route.routeId;
                        gcmMessage.messagePriority = MessagePriority.PRIORITY_LOW;
                        break;

                    // Disruption message.
                    case TYPE_DISRUPTION:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = alert.route.routeId;
                        break;

                    // High priority Weather GCM message.
                    case TYPE_WEATHER:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = CommuteMessageType.ALERT_CURRENT_MESSAGE.value;
                        gcmMessage.messagePriority = MessagePriority.PRIORITY_HIGH;
                        break;

                    // High priority Weather GCM message.
                    case TYPE_APP:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_APP_MESSAGE.value);
                        gcmMessage.collapseKey = CommuteMessageType.ALERT_APP_MESSAGE.value;
                        break;

                    case TYPE_MAINTENANCE:
                        gcmMessage.addData(MessageRootKey.MESSAGE_TYPE.value, CommuteMessageType.ALERT_CURRENT_MESSAGE.value);
                        gcmMessage.collapseKey = alert.route.routeId;
                        break;
                }
            }
        }
        return gcmMessage;
    }

    /**
     * Add platform accounts to a message.
     *
     * @param message          message to add platform accounts to
     * @param platformAccounts list of platform accounts for message.
     * @return Message populated with accounts, or null if no accounts.
     */
    private static Message addMessagePlatformAccounts(@Nonnull Message message,
                                                      @Nonnull List<PlatformAccount> platformAccounts) {
        // Add accounts to the message.
        for (PlatformAccount account : platformAccounts) {
            if (account.platform != null && account.platform.endpointUrl != null) {
                Credentials credentials = new Credentials();
                credentials.authorizationKey = account.authorizationKey;
                credentials.endpointUrl = account.platform.endpointUrl;
                credentials.packageUri = account.packageUri;
                credentials.certificateBody = account.certificateBody;

                message.credentials = credentials;
                message.platformType = account.platform.platformType;
            }
        }

        // If there are no platform accounts, return null.
        if (message.credentials == null || message.credentials.endpointUrl == null) {
            return null;
        }
        return message;
    }

    /**
     * Clone a message completely without any device information
     *
     * @param message message to copy.
     * @return a cloned message with removed recipient information.
     */
    @Nonnull
    public static Message cloneMessage(@Nonnull Message message) {
        Message clonedMessage = new Message();
        clonedMessage.id = message.id;
        clonedMessage.credentials = message.credentials;
        clonedMessage.recipients = new ArrayList<>();
        clonedMessage.payloadData = message.payloadData;
        clonedMessage.collapseKey = message.collapseKey;
        clonedMessage.ttl = message.ttl;
        clonedMessage.isDryRun = message.isDryRun;
        clonedMessage.shouldDelayWhileIdle = message.shouldDelayWhileIdle;
        clonedMessage.messagePriority = message.messagePriority;
        clonedMessage.task = message.task;
        clonedMessage.sentTime = message.sentTime;

        return clonedMessage;
    }
}
