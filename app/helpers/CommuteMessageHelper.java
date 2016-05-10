package helpers;

import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import pushservices.enums.MessagePriority;
import pushservices.enums.PlatformType;
import pushservices.helpers.MessageBuilder;
import pushservices.models.database.Credentials;
import pushservices.models.database.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to easily create relevant push platform messages for different app alert types,
 * which can be sent out using the dispatchers.
 */
public class CommuteMessageHelper {

    /*
     * Different types of commute push message types.
     */
    private enum MessageType {
        TYPE_CURRENT_MESSAGE("alert_type", "current_message"),
        TYPE_DETOUR_MESSAGE("alert_type", "detour_message"),
        TYPE_ADVISORY_MESSAGE("alert_type", "advisory_message"),
        TYPE_APP_MESSAGE("alert_type", "app_message"),
        TYPE_ALERT_CANCEL("alert_type", "cancel_message"),
        TYPE_REGISTRATION("alert_type", "registered_on_network"),
        TYPE_RESEND_SUBSCRIPTIONS("alert_type", "resend_subscriptions");

        private String key;
        private String value;
        MessageType(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    /*
     * Message payload data keys for Alert message.
     */
    private enum AlertMessageKey {
        KEY_ROUTE_ID("route_id"),
        KEY_ROUTE_NAME("route_name"),
        KEY_ROUTE_MESSAGE("message");

        private String name;
        AlertMessageKey(String name) {
            this.name = name;
        }
    }

    /**
     * Build a list of commute update (detour, advisory, etc), or route cancellation push messages
     * with a set of recipients for an updated route.
     *
     * @param route           route which has been updated.
     * @param platformAccount platform account for the alert message.
     * @return List of platform messages for route.
     */
    public static List<Message> getAlertMessages(@Nonnull Route route, @Nonnull List<Device> devices,
                                                 @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();
        if (route.alerts != null && !route.alerts.isEmpty()) {
            // If there are route alerts, create messages for each alert in the route.
            return buildAlertUpdateMessage(route, devices, platformAccount);

        } else {
            // If the alerts list is empty or null, this route is cancelled.
            return buildAlertCancelMessage(route, devices, platformAccount);
        }
    }

    /**
     * Build a GCM device confirmation push message.
     *
     * @param device          device to send to.
     * @param platformAccount push service platform accounts.
     * @return Message.
     */
    @Nullable
    public static Message buildDeviceRegisteredMessage(@Nonnull Device device, @Nonnull PlatformAccount platformAccount) {
        Credentials credentials = getMessageCredentials(platformAccount);
        if (device.token != null && credentials != null) {
            return new MessageBuilder.Builder()
                    .setCollapseKey(MessageType.TYPE_REGISTRATION.value)
                    .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                    .setPlatformCredentials(credentials)
                    .addDeviceToken(device.token)
                    .putData(MessageType.TYPE_REGISTRATION.key, MessageType.TYPE_REGISTRATION.value)
                    .build();
        }
        return null;
    }

    /**
     * Build a list of commute update (detour, advisory, etc) with a set of recipients
     * for an updated route.
     *
     * @param route           route which has been updated.
     * @param platformAccount platform account for the alert message.
     * @return List of platform messages for route.
     */
    @Nonnull
    private static List<Message> buildAlertUpdateMessage(@Nonnull Route route, @Nonnull List<Device> devices,
                                                         @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();

        Credentials credentials = getMessageCredentials(platformAccount);
        if (credentials != null && route.alerts != null && route.routeId != null) {
            for (Alert alert : route.alerts) {

                MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                        .setCollapseKey(route.routeId)
                        .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                        .setPlatformCredentials(credentials)
                        .putData(AlertMessageKey.KEY_ROUTE_ID.name, route.routeId)
                        .putData(AlertMessageKey.KEY_ROUTE_NAME.name, route.routeName)
                        .putData(AlertMessageKey.KEY_ROUTE_MESSAGE.name, alert.messageBody);

                for (Device device : devices) {
                    messageBuilder.addDeviceToken(device.token);
                }

                switch (alert.type) {
                    case TYPE_DETOUR:
                        messageBuilder.putData(MessageType.TYPE_DETOUR_MESSAGE.key, MessageType.TYPE_DETOUR_MESSAGE.value);
                        break;

                    case TYPE_INFORMATION:
                        messageBuilder.putData(MessageType.TYPE_ADVISORY_MESSAGE.key, MessageType.TYPE_ADVISORY_MESSAGE.value);
                        break;

                    case TYPE_DISRUPTION:
                        messageBuilder.putData(MessageType.TYPE_CURRENT_MESSAGE.key, MessageType.TYPE_CURRENT_MESSAGE.value);
                        break;

                    case TYPE_WEATHER:
                        messageBuilder.putData(MessageType.TYPE_CURRENT_MESSAGE.key, MessageType.TYPE_CURRENT_MESSAGE.value);
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_HIGH);
                        break;

                    case TYPE_APP:
                        messageBuilder.putData(MessageType.TYPE_APP_MESSAGE.key, MessageType.TYPE_APP_MESSAGE.value);
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_HIGH);
                        break;

                    case TYPE_MAINTENANCE:
                        messageBuilder.putData(MessageType.TYPE_CURRENT_MESSAGE.key, MessageType.TYPE_CURRENT_MESSAGE.value);
                        break;
                }
                messages.add(messageBuilder.build());
            }
        }
        return messages;
    }

    /**
     * Build a list of commute cancel messages with a set of recipients for an
     * updated route.
     *
     * @param route           route which has been updated.
     * @param platformAccount platform account for the alert message.
     * @return List of platform messages for route.
     */
    @Nullable
    private static List<Message> buildAlertCancelMessage(@Nonnull Route route, @Nonnull List<Device> devices,
                                                         @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();

        Credentials credentials = getMessageCredentials(platformAccount);
        if (credentials != null && route.routeId != null) {
            MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                    .setCollapseKey(route.routeId)
                    .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                    .setPlatformCredentials(credentials)
                    .putData(AlertMessageKey.KEY_ROUTE_ID.name, route.routeId)
                    .putData(MessageType.TYPE_ALERT_CANCEL.key, MessageType.TYPE_ALERT_CANCEL.value);

            for (Device device : devices) {
                messageBuilder.addDeviceToken(device.token);
            }
            messages.add(messageBuilder.build());
        }
        return messages;
    }

    /**
     * Get credentials for a platform Account.
     *
     * @param account platform  credentials for message.
     * @return push-service credentials model populated with platform specific keys, etc.
     */
    private static Credentials getMessageCredentials(@Nonnull PlatformAccount account) {
        if (account.platformType != null) {
            Credentials credentials = new Credentials();
            credentials.platformType = PlatformType.SERVICE_GCM;
            credentials.authorisationKey = account.authorisationKey;
            credentials.packageUri = account.packageUri;
            credentials.certificateBody = account.certificateBody;
            return credentials;
        }
        return null;
    }

}
