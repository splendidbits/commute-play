package helpers;

import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import helpers.pushservices.MessageBuilder;
import models.AlertModifications;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.PayloadElement;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import services.fluffylog.Logger;

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
public class AlertHelper {
    private static final int MAX_MESSAGE_PAYLOAD_LENGTH = 1500;
    private static final int ALERT_BODY_MINIMUM_LENGTH = 12;

    /**
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

    /**
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
     * Strip a string of HTML but preserve all kinds of line-breaks.
     *
     * @param agency agency to parse
     */
    public static void parseHtml(@Nonnull Agency agency) {
        if (agency.routes != null) {
            for (Route route : agency.routes) {

                if (route.routeName != null && route.routeName.isEmpty()) {
                    route.routeName = fixCommonHtmlIssues(route.routeName);
                }

                if (route.alerts != null) {
                    for (Alert alert : route.alerts) {

                        if (alert.messageTitle != null && !alert.messageTitle.isEmpty()) {
                            alert.messageTitle = fixCommonHtmlIssues(alert.messageTitle);
                        }

                        if (alert.messageSubtitle != null && !alert.messageSubtitle.isEmpty()) {
                            alert.messageSubtitle = fixCommonHtmlIssues(alert.messageSubtitle);
                        }

                        if (alert.messageBody != null && !alert.messageBody.isEmpty()) {
                            alert.messageBody = fixCommonHtmlIssues(alert.messageBody);
                        }

                        if (alert.locations != null) {
                            for (Location location : alert.locations) {

                                if (location.name != null && !location.name.isEmpty()) {
                                    location.name = fixCommonHtmlIssues(location.name);
                                }
                                if (location.message != null && !location.message.isEmpty()) {
                                    location.message = fixCommonHtmlIssues(location.message);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Strip complex HTML from a string but preserve bold, italics, newlines and whitesspace.
     *
     * @param input string to strip html from.
     * @return original string minus dom syntax but keeping whitespace.
     */
    private static String fixCommonHtmlIssues(@Nonnull String input) {
        // Strip complex html.
        input = Jsoup.clean(input, "", Whitelist.basic(), new Document.OutputSettings().prettyPrint(false));

        // Remove any tabs special characters.
        input = input.replace("\t", "");

        // Remove whitespace at start and end of string to prepare for the next loop.
        input = input.trim();

        return input;
    }

    /**
     * Build a list of commute update (detour, advisory, etc), or route cancellation push messages
     * with a set of recipients for an updated route.
     *
     * @param route           route which has been updated.
     * @param platformAccount platform account for the alert message.
     * @param isCancellation  set whether the alert message is an update or cancellation (clear).
     * @return List of platform messages for route.
     */
    public static List<Message> getAlertMessages(@Nonnull Route route, @Nonnull List<Device> devices,
                                                 @Nonnull PlatformAccount platformAccount, boolean isCancellation) {

        // Build the message but truncate messages that are too long to avoid MessageTooBig errors.
        if (!isCancellation) {
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

            return new MessageBuilder.Builder()
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
                    MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                            .setCollapseKey(route.routeId)
                            .setPlatformCredentials(credentials)
                            .setMessagePriority(alert.highPriority ? MessagePriority.PRIORITY_HIGH : MessagePriority.PRIORITY_NORMAL)
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
                            break;

                        case TYPE_IN_APP:
                            messageBuilder.putData(MessageType.TYPE_APP_MESSAGE.key, MessageType.TYPE_APP_MESSAGE.value);
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
    @NotNull
    private static List<Message> buildAlertCancelMessage(@Nonnull Route route, @Nonnull List<Device> devices,
                                                         @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();
        if (route.routeId != null) {
            Credentials credentials = getMessageCredentials(platformAccount);

            if (credentials != null) {
                MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
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
     */
    private static void truncateMessagePayload(@Nonnull Message message, int truncateAmount) {
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
            credentials.authKey = account.authorisationKey;
            credentials.packageUri = account.packageUri;
            credentials.certBody = account.certificateBody;
            return credentials;
        }
        return null;
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param existingAgency the currently saved agency.
     * @param freshAgency    the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    @Nonnull
    public static AlertModifications getAgencyModifications(Agency existingAgency, Agency freshAgency) {
        // Both agencies do not exist. This is bad.
        if (existingAgency == null && freshAgency == null) {
            Logger.warn("Both agencies for modifications calculation were null!");
            return new AlertModifications(-1);
        }

        // Copy the routes.
        List<Route> existingRoutes = existingAgency != null
                ? existingAgency.routes
                : new ArrayList<>();

        List<Route> freshRoutes = freshAgency != null
                ? freshAgency.routes
                : new ArrayList<>();

        int agencyId = freshAgency != null
                ? freshAgency.id
                : existingAgency.id;

        AlertModifications alertModifications = new AlertModifications(agencyId);

        // Both existing and fresh routes are null.
        if (freshRoutes == null && existingRoutes == null) {
            return alertModifications;
        }

        // Fresh agency routes exist while there are no Existing routes.
        if ((existingRoutes == null || existingRoutes.isEmpty()) && (freshRoutes != null && !freshRoutes.isEmpty())) {
            Logger.info(String.format("No existing routes for agency %d. Adding all routes as updated.", agencyId));

            for (Route freshRoute : freshRoutes) {
                if (freshRoute.alerts != null) {
                    for (Alert freshAlert : freshRoute.alerts) {
                        alertModifications.addUpdatedAlert(freshAlert);
                    }
                }
            }
            return alertModifications;
        }

        // Existing agency routes exists and there are no Fresh routes.
        if ((freshRoutes == null || freshRoutes.isEmpty()) && (existingRoutes != null && !existingRoutes.isEmpty())) {
            Logger.info(String.format("No new fresh routes for agency %d. Marking all existing as stale.", agencyId));

            for (Route existingRoute : existingRoutes) {
                if (existingRoute.alerts != null) {
                    for (Alert existingAlert : existingRoute.alerts) {
                        alertModifications.addStaleAlert(existingAlert);
                    }
                }
            }
            return alertModifications;
        }

        // Find updated / new fresh alerts.
        if (freshRoutes != null) {
            for (Route freshRoute : freshRoutes) {

                boolean existingRouteExists = false;
                for (Route existingRoute : existingRoutes) {

                    // If the routes match.
                    if (freshRoute.routeId.equals(existingRoute.routeId)) {
                        existingRouteExists = true;

                        List<Alert> updatedAlerts = getUpdatedAlerts(existingRoute.alerts, freshRoute.alerts);
                        for (Alert updatedAlert : updatedAlerts) {
                            alertModifications.addUpdatedAlert(updatedAlert);
                        }

                        // There was a route match so skip the inner loop.
                        break;
                    }
                }

                // The fresh route does not exist at all. Add all alerts as updated.
                if (!existingRouteExists) {
                    if (freshRoute.alerts != null) {
                        for (Alert freshAlert : freshRoute.alerts) {
                            alertModifications.addUpdatedAlert(freshAlert);
                        }
                    }
                }
            }
        }

        // Find stale existing alerts.
        if (existingRoutes != null) {
            for (Route existingRoute : existingRoutes) {

                boolean freshRouteExists = false;
                for (Route freshRoute : freshRoutes) {

                    // If the routes match.
                    if (freshRoute.routeId.equals(existingRoute.routeId)) {
                        freshRouteExists = true;

                        List<Alert> staleAlerts = getStaleAlerts(existingRoute.alerts, freshRoute.alerts, alertModifications);
                        for (Alert staleAlert : staleAlerts) {
                            alertModifications.addStaleAlert(staleAlert);
                        }

                        // There was a route match so skip the inner loop.
                        break;
                    }
                }

                // The existing route was deleted. Mark all as stale
                if (!freshRouteExists) {
                    if (existingRoute.alerts != null) {
                        for (Alert existingAlert : existingRoute.alerts) {
                            alertModifications.addStaleAlert(existingAlert);
                        }
                    }
                }
            }
        }

        return alertModifications;
    }

    /**
     * Get a list of fresh (new) alerts (not including stale) for a route.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of new route alerts.
     */
    @Nonnull
    private static List<Alert> getUpdatedAlerts(List<Alert> existingAlerts, List<Alert> freshAlerts) {
        List<Alert> updatedAlerts = new ArrayList<>();

        // If there are no fresh alerts alerts, nothing can be updated.
        if ((freshAlerts == null) || freshAlerts.isEmpty()) {
            return new ArrayList<>();
        }

        // If all existing alerts are empty mark all fresh as updated.
        if (existingAlerts == null || existingAlerts.isEmpty()) {
            return freshAlerts;
        }

        // Iterate through each fresh alert. Add it as updated if either of the following are true:
        // 1: The fresh alert does not exist in the existing alerts
        for (Alert freshAlert : freshAlerts) {
            if (!existingAlerts.contains(freshAlert)) {
                updatedAlerts.add(freshAlert);
            }
        }

        return updatedAlerts;
    }

    /**
     * An stale alert is defined as an existing {@link enums.AlertType} for the route which no longer
     * exists in the fresh route alert types.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of stale route alerts.
     */
    @Nonnull
    private static List<Alert> getStaleAlerts(List<Alert> existingAlerts, List<Alert> freshAlerts, AlertModifications alertModifications) {
        List<Alert> staleAlerts = new ArrayList<>();

        if (existingAlerts == null && freshAlerts == null) {
            return new ArrayList<>();
        }

        // If all fresh alerts are "empty" mark all existing as stale.
        if ((freshAlerts == null || freshAlerts.isEmpty()) &&
                (existingAlerts != null && !existingAlerts.isEmpty())) {
            return existingAlerts;
        }

        // Ensure the AlertModifications *updates* don't contain any of the fresh alert types.
        if (existingAlerts != null && freshAlerts != null) {
            for (Alert existingAlert : existingAlerts) {
                boolean existingAlertsHasFreshAlertType = false;

                for (Alert modificationUpdateAlert : alertModifications.getUpdatedAlerts()) {
                    if (modificationUpdateAlert.type.equals(existingAlert.type)) {
                        existingAlertsHasFreshAlertType = true;
                        break;
                    }
                }

                if (!existingAlertsHasFreshAlertType) {
                    if (!freshAlerts.contains(existingAlert)) {
                        staleAlerts.add(existingAlert);
                    }
                }
            }
        }

        return staleAlerts;
    }
}
