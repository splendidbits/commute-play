package helpers;

import enums.AlertType;
import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import helpers.pushservices.PlatformMessageBuilder;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.Credentials;
import models.pushservices.Message;
import models.pushservices.PayloadElement;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Used to easily create relevant push platform messages for different app alert types,
 * which can be sent out using the dispatchers.
 */
public class CommuteAlertHelper {
    private static final int MAX_MESSAGE_PAYLOAD_LENGTH = 2048;
    private static final int ALERT_BODY_MINIMUM_LENGTH = 12;

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

        private String key;

        AlertMessageKey(String key) {
            this.key = key;
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
        if (route.alerts != null && !route.alerts.isEmpty()) {

            // Build the message but truncate messages that are too long to avoid MessageTooBig errors.
            List<Message> messages = buildAlertUpdateMessage(route, devices, platformAccount);
            for (Message message : messages) {
                int payloadLength = messagePayloadCount(message);

                if (payloadLength >= MAX_MESSAGE_PAYLOAD_LENGTH) {
                    int truncateAmount = payloadLength - MAX_MESSAGE_PAYLOAD_LENGTH;
                    truncateMessagePayload(message, truncateAmount);
                }
            }
            return messages;

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
            Set<String> tokens = new HashSet<>();
            tokens.add(device.token);

            return new PlatformMessageBuilder.Builder()
                    .setCollapseKey(MessageType.TYPE_REGISTRATION.value)
                    .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                    .setPlatformCredentials(credentials)
                    .setDeviceTokens(tokens)
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
        if (route.alerts != null && route.routeId != null) {

            for (Alert alert : route.alerts) {
                Credentials credentials = getMessageCredentials(platformAccount);

                if (credentials != null) {
                    PlatformMessageBuilder.Builder messageBuilder = new PlatformMessageBuilder.Builder()
                            .setCollapseKey(route.routeId)
                            .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                            .setPlatformCredentials(credentials)
                            .putData(AlertMessageKey.KEY_ROUTE_ID.key, route.routeId)
                            .putData(AlertMessageKey.KEY_ROUTE_NAME.key, route.routeName)
                            .putData(AlertMessageKey.KEY_ROUTE_MESSAGE.key, alert.messageBody);

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

                    Set<String> tokenSet = new HashSet<>();
                    for (Device device : devices) {
                        tokenSet.add(device.token);
                    }

                    messageBuilder.setDeviceTokens(tokenSet);
                    messages.add(messageBuilder.build());

                } else {
                    Logger.error("No Credentials model found for update message.");
                }
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
        if (route.routeId != null) {
            Credentials credentials = getMessageCredentials(platformAccount);

            if (credentials != null) {
                PlatformMessageBuilder.Builder messageBuilder = new PlatformMessageBuilder.Builder()
                        .setCollapseKey(route.routeId)
                        .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                        .setPlatformCredentials(credentials)
                        .putData(AlertMessageKey.KEY_ROUTE_ID.key, route.routeId)
                        .putData(MessageType.TYPE_ALERT_CANCEL.key, MessageType.TYPE_ALERT_CANCEL.value);

                Set<String> tokenSet = new HashSet<>();
                for (Device device : devices) {
                    tokenSet.add(device.token);
                }

                messageBuilder.setDeviceTokens(tokenSet);
                messages.add(messageBuilder.build());

            } else {
                Logger.error("No Credentials model found for cancel message.");
            }
        }
        return messages;
    }

    /**
     * Return a byte count of the total length of the message payload.
     *
     * @param message the message to count the contents of.
     * @return size of all fields in the payload.
     */
    private static int messagePayloadCount(@Nonnull Message message) {
        List<PayloadElement> messagePayload = message.payloadData;
        final int controlCharCount = 6; // {, ", }, ,

        int payloadByteCount = 0;
        if (messagePayload != null) {

            for (PayloadElement element : messagePayload) {
                payloadByteCount += element.key.length();
                payloadByteCount += element.value.length();
                payloadByteCount += controlCharCount;
            }
        }
        return payloadByteCount;
    }

    /**
     * Truncate message payload values so that a GCM message fits with the provider. This should only
     * attempt to truncate the route message at the moment.
     *
     * @param message        message to truncate.
     * @param truncateAmount amount to reduce the payload contents by.
     * @return true if the message could be fully trimmed to the length of lengthToTruncate;
     */
    private static boolean truncateMessagePayload(@Nonnull Message message, int truncateAmount) {
        List<PayloadElement> messagePayload = message.payloadData;
        if (messagePayload != null && isAlertMessage(message) && truncateAmount > 0) {

            for (PayloadElement element : messagePayload) {
                String messageBody = element.value;

                // First, try to trim the alert message itself as much as possible.
                if (element.key.equals(AlertMessageKey.KEY_ROUTE_MESSAGE.key) && !messageBody.isEmpty()) {

                    /*
                     * The payload message can't just be truncated by amount sent, as this may
                     * encroach into the important minimum body side shown. Instead work out how much
                     * the predicted slice would encroach on ALERT_BODY_MINIMUM_LENGTH (or "overslice") by.
                     */
                    int oversliceAmount = 0;
                    int remainderAfterSlice = messageBody.length() - truncateAmount;

                    if (remainderAfterSlice < 1) {
                        oversliceAmount = ALERT_BODY_MINIMUM_LENGTH;
                    } else if (remainderAfterSlice < ALERT_BODY_MINIMUM_LENGTH) {
                        oversliceAmount = ALERT_BODY_MINIMUM_LENGTH - remainderAfterSlice;
                    }

                    // Now calculate the exact length to truncate.
                    truncateAmount = (truncateAmount - oversliceAmount);
                    int sliceAnchorEnd = messageBody.length() - (truncateAmount - oversliceAmount);

                    // Truncate the model in place.
                    element.value = messageBody.substring(0, sliceAnchorEnd);

                    Logger.warn(String.format("Sliced %1$d bytes off message type %2$s payload end.",
                            (truncateAmount - oversliceAmount), message.collapseKey));
                }
            }
        }
        return truncateAmount < 1;
    }

    /**
     * Ascertain if the message is a new or updated agency alert message.
     *
     * @param message the message to check.
     * @return true if the message an agency alert message.
     */
    private static boolean isAlertMessage(@Nonnull Message message) {
        List<PayloadElement> messagePayload = message.payloadData;
        if (messagePayload != null) {
            for (PayloadElement element : messagePayload) {
                if (element.key.equals(MessageType.TYPE_CURRENT_MESSAGE.key) ||
                        element.key.equals(MessageType.TYPE_DETOUR_MESSAGE.key) ||
                        element.key.equals(MessageType.TYPE_ADVISORY_MESSAGE.key)) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Check if the Alert is "empty" (if there is no message, or type.
     *
     * @param alert the alert to check.
     * @return true if the message is empty.
     */
    public static boolean isAlertEmpty(@Nonnull Alert alert) {
        return !(alert.messageBody != null &&
                !alert.messageBody.isEmpty()) ||
                alert.type == null || AlertType.TYPE_NONE.equals(alert.type);
    }

    /**
     * Deep copy a list of Routes including all of its children.
     *
     * @param routes {@link List<Route>} list to copy.
     * @return copied {@link List<Route>}.
     */
    public static List<Route> copyRoutes(List<Route> routes) {
        List<Route> copiedRoutes = new ArrayList<>();
        if (routes != null) {
            for (Route route : routes) {
                try {
                    copiedRoutes.add(copyRoutes(route));
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        return copiedRoutes;
    }

    /**
     * Deep copy a route including all of its children. upward
     * relationships such as subscriptions, agencies etc are not deep copied.
     *
     * @param route {@link Route} to copy.
     * @return copied {@link Route}.
     */
    @SuppressWarnings("WeakerAccess")
    public static Route copyRoutes(@Nonnull Route route) throws CloneNotSupportedException {
        return (Route) route.clone();
    }
}
