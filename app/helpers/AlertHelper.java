package helpers;

import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
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
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Used to easily create relevant push platform messages for different app alert types,
 * which can be sent out using the dispatchers.
 */
public class AlertHelper {
    private static final int MAX_MESSAGE_PAYLOAD_LENGTH = 1500;
    private static final int ALERT_BODY_MINIMUM_LENGTH = 12;

    /*
     * Message TTLs directly correlate with how GCM delivers a message. For example,
     * messages with a longer TTL will be de-prioritised (throttled) but they will expunged
     * after a certain period of time.
     */
    private static final int ALERT_SHORT_TTL = 60 * 60 * 1; // 1 hour
    private static final int ALERT_LONG_TTL = 60 * 60 * 48; // 48 hours

    /**
     * Different types of commute push message types.
     */
    private static final String MESSAGE_TYPE_KEY = "message_type";

    private enum MessageType {
        TYPE_MESSAGE_NOTIFY("message_notify"),
        TYPE_MESSAGE_CANCEL("message_cancel"),
        TYPE_REGISTRATION_COMPLETE("registered_on_network"),
        TYPE_RESEND_SUBSCRIPTIONS("resend_subscriptions");

        private String value;

        MessageType(String value) {
            this.value = value;
        }
    }

    /**
     * Message payload data keys for Alert message.
     */
    private enum AlertMessageKey {
        KEY_ALERT_ROUTE_ID("route_id"),
        KEY_ALERT_ROUTE_NAME("route_name"),
        KEY_ALERT_CATEGORY("alert_category"),
        KEY_ALERT_MESSAGE("alert_message");

        private String value;

        AlertMessageKey(String value) {
            this.value = value;
        }
    }

    /**
     * Iterate through all alerts in all routes, and add the route parent model to each
     * alert -> route relation.
     *
     * @param agency The specified agency to fill alerts with routes.
     */
    public static void populateBackReferences(Agency agency) {
        if (agency != null && agency.routes != null) {
            for (Route route : agency.routes) {
                if (route.agency == null) {
                    route.agency = agency;
                    populateBackReferences(route);
                }
            }
        }
    }

    public static void populateBackReferences(Route route) {
        if (route != null && route.alerts != null) {
            for (Alert alert : route.alerts) {
                if (alert.route == null) {
                    alert.route = route;
                    populateBackReferences(alert);
                }
            }
        }
    }

    public static void populateBackReferences(Alert alert) {
        if (alert != null && alert.locations != null) {
            for (Location location : alert.locations) {
                if (location.alert == null) {
                    location.alert = alert;
                }
            }
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
     * @param alert           route which has been updated.
     * @param platformAccount platform account for the alert message.
     * @param isCancellation  set whether the alert message is an update or cancellation (clear).
     * @return List of platform messages for route.
     */
    public static List<Message> getAlertMessages(@Nonnull Alert alert, @Nonnull List<Device> devices,
                                                 @Nonnull PlatformAccount platformAccount, boolean isCancellation) {

        // Build the message but truncate messages that are too long to avoid MessageTooBig errors.
        List<Message> messages;
        if (!isCancellation) {
            messages = buildAlertUpdateMessage(alert, devices, platformAccount);

        } else {
            // If the alerts list is empty or null, this route is cancelled.
            messages = buildAlertCancelMessage(alert, devices, platformAccount);
        }

        for (Message message : messages) {
            int payloadLength = messagePayloadCount(message);
            if (payloadLength >= MAX_MESSAGE_PAYLOAD_LENGTH) {
                int truncateAmount = payloadLength - MAX_MESSAGE_PAYLOAD_LENGTH;
                truncateMessagePayload(message, truncateAmount);
            }
        }
        return messages;
    }

    /**
     * Build a set of messages which tells clients to re-subscribe. (re-register and resend subscriptions).
     *
     * @param devices         list of devices.
     * @param platformAccount platform account.
     * @return list of messages to send.
     */
    public static List<Message> getResubscribeMessages(@Nonnull List<Device> devices, @Nonnull PlatformAccount platformAccount) {
        return buildResubscribeMessage(devices, platformAccount);
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

            try {
                return new MessageBuilder.Builder()
                        .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                        .setTimeToLiveSeconds(ALERT_LONG_TTL)
                        .setPlatformCredentials(credentials)
                        .setDeviceTokens(tokens)
                        .putData(MESSAGE_TYPE_KEY, MessageType.TYPE_REGISTRATION_COMPLETE.value)
                        .build();

            } catch (TaskValidationException e) {
                Logger.error("Exception building the device_registration message.");
            }
        }
        return null;
    }

    /**
     * Build a list of commute update (detour, advisory, etc) with a set of recipients
     * for an updated route.
     *
     * @param alert           alert which has been updated.
     * @param platformAccount platform account for the alert message.
     * @return List of platform messages for route.
     */
    @Nonnull
    private static List<Message> buildAlertUpdateMessage(@Nonnull Alert alert, @Nonnull List<Device> devices,
                                                         @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();

        if (alert.route != null) {
            Credentials credentials = getMessageCredentials(platformAccount);

            if (credentials != null) {
                MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                        .setCollapseKey(alert.route.routeId + "-" + alert.type.name())
                        .setPlatformCredentials(credentials)
                        .putData(MESSAGE_TYPE_KEY, MessageType.TYPE_MESSAGE_NOTIFY.value)
                        .putData(AlertMessageKey.KEY_ALERT_ROUTE_ID.value, alert.route.routeId)
                        .putData(AlertMessageKey.KEY_ALERT_ROUTE_NAME.value, alert.route.routeName)
                        .putData(AlertMessageKey.KEY_ALERT_CATEGORY.value, alert.type.name())
                        .putData(AlertMessageKey.KEY_ALERT_MESSAGE.value, alert.messageBody);

                switch (alert.type) {
                    case TYPE_DETOUR:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_SHORT_TTL);
                        break;

                    case TYPE_INFORMATION:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_LONG_TTL);
                        break;

                    case TYPE_DISRUPTION:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_SHORT_TTL);
                        break;

                    case TYPE_WEATHER:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_LONG_TTL);
                        break;

                    case TYPE_IN_APP:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_LONG_TTL);
                        break;

                    case TYPE_MAINTENANCE:
                        messageBuilder.setMessagePriority(MessagePriority.PRIORITY_NORMAL);
                        messageBuilder.setTimeToLiveSeconds(ALERT_SHORT_TTL);
                        break;
                }

                Set<String> tokenSet = new HashSet<>();
                for (Device device : devices) {
                    tokenSet.add(device.token);
                }

                messageBuilder.setDeviceTokens(tokenSet);
                try {
                    messages.add(messageBuilder.build());
                } catch (TaskValidationException e) {
                    Logger.error("Exception building the alert update message.");
                }
            } else {
                Logger.error("No Credentials model found for update message.");
            }
        }
        return messages;
    }

    /**
     * Build a list of commute cancel messages with a set of recipients for an
     * updated route.
     *
     * @param alert           alert which has been cancelled.
     * @param platformAccount platform account for the alert message.
     * @return List of platform messages for route.
     */
    @NotNull
    private static List<Message> buildAlertCancelMessage(@Nonnull Alert alert,
                                                         @Nonnull List<Device> devices,
                                                         @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();
        Credentials credentials = getMessageCredentials(platformAccount);

        if (credentials != null) {
            MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                    .setCollapseKey(alert.route.routeId + "-" + alert.type.name())
                    .setPlatformCredentials(credentials)
                    .putData(MESSAGE_TYPE_KEY, MessageType.TYPE_MESSAGE_CANCEL.value)
                    .putData(AlertMessageKey.KEY_ALERT_ROUTE_ID.value, alert.route.routeId)
                    .putData(AlertMessageKey.KEY_ALERT_CATEGORY.value, alert.type.name());

            Set<String> tokenSet = new HashSet<>();
            for (Device device : devices) {
                tokenSet.add(device.token);
            }

            messageBuilder.setDeviceTokens(tokenSet);

            try {
                messages.add(messageBuilder.build());
            } catch (TaskValidationException e) {
                Logger.error("Exception building the alert cancellation message.");
            }

        } else {
            Logger.error("No Credentials model found for cancel message.");
        }
        return messages;
    }

    @NotNull
    private static List<Message> buildResubscribeMessage(@Nonnull List<Device> devices, @Nonnull PlatformAccount platformAccount) {
        List<Message> messages = new ArrayList<>();
        Credentials credentials = getMessageCredentials(platformAccount);

        if (credentials != null) {
            MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                    .setPlatformCredentials(credentials)
                    .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                    .setTimeToLiveSeconds(ALERT_LONG_TTL)
                    .putData("alert_type", MessageType.TYPE_RESEND_SUBSCRIPTIONS.value)
                    .putData(MESSAGE_TYPE_KEY, MessageType.TYPE_RESEND_SUBSCRIPTIONS.value);

            Set<String> tokenSet = new HashSet<>();
            for (Device device : devices) {
                tokenSet.add(device.token);
            }

            messageBuilder.setDeviceTokens(tokenSet);

            try {
                messages.add(messageBuilder.build());
            } catch (TaskValidationException e) {
                Logger.error("Exception building the alert cancellation message.");
            }

        } else {
            Logger.error("No Credentials model found for cancel message.");
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
                if (element.equals(AlertMessageKey.KEY_ALERT_MESSAGE) && !messageBody.isEmpty()) {

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
                if (element.equals(MessageType.TYPE_MESSAGE_NOTIFY)) {
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
        Map<String, Set<enums.AlertType>> routeIdAlertTypes = new HashMap<>();
        Set<Alert> ignoredAlerts = new HashSet<>();

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
                        String routeId = freshRoute.routeId;
                        existingRouteExists = true;

                        List<Alert> updatedAlerts = getUpdatedAlerts(existingRoute.alerts, freshRoute.alerts, ignoredAlerts);
                        for (Alert updatedAlert : updatedAlerts) {

                            // Add the alert as an update.
                            alertModifications.addUpdatedAlert(updatedAlert);

                            // Add the alert type to the route alert update types list.
                            Set<enums.AlertType> updateRoutedAlertTypes = routeIdAlertTypes.containsKey(routeId)
                                    ? routeIdAlertTypes.get(routeId)
                                    : new HashSet<>();

                            updateRoutedAlertTypes.add(updatedAlert.type);
                            routeIdAlertTypes.put(routeId, updateRoutedAlertTypes);
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
                        String routeId = freshRoute.routeId;
                        freshRouteExists = true;

                        List<Alert> staleAlerts = getStaleAlerts(existingRoute.alerts, freshRoute.alerts, ignoredAlerts);
                        for (Alert staleAlert : staleAlerts) {

                            // Only add the stale alert if the same alert type has not been marked as updated.
                            Set<enums.AlertType> updatedAlertTypes = routeIdAlertTypes.containsKey(routeId)
                                    ? routeIdAlertTypes.get(routeId)
                                    : new HashSet<>();

                            if (!updatedAlertTypes.contains(staleAlert.type)) {
                                alertModifications.addStaleAlert(staleAlert);
                            }
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
     * Get a list of fresh (new) alerts for a route.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of new route alerts.
     */
    @Nonnull
    private static List<Alert> getUpdatedAlerts(List<Alert> existingAlerts, List<Alert> freshAlerts, Set<Alert> ignoredAlerts) {
        List<Alert> updatedAlerts = new ArrayList<>();

        if (CompareUtils.isEquals(existingAlerts, freshAlerts)) {
            return updatedAlerts;
        }

        // Iterate through and add each updated alert that did not already exist before.
        for (Alert freshAlert : freshAlerts) {

            // If everything matches in the alerts, but the locations have been
            // deleted, do not treat as an update, but rather skip the alert altogether.
            for (Alert existingAlert : existingAlerts) {
                Alert existingAlertNoLocations = new Alert();
                existingAlertNoLocations.locations = new ArrayList<>();
                existingAlertNoLocations.messageTitle = existingAlert.messageTitle;
                existingAlertNoLocations.messageSubtitle = existingAlert.messageSubtitle;
                existingAlertNoLocations.messageBody = existingAlert.messageBody;
                existingAlertNoLocations.type = existingAlert.type;
                existingAlertNoLocations.route = existingAlert.route;
                existingAlertNoLocations.externalUri = existingAlert.externalUri;
                existingAlertNoLocations.highPriority = existingAlert.highPriority;
                existingAlertNoLocations.lastUpdated = existingAlert.lastUpdated;

                if (freshAlert.equals(existingAlertNoLocations)) {
                    ignoredAlerts.add(freshAlert);
                }
            }

            if (!existingAlerts.contains(freshAlert) && !ignoredAlerts.contains(freshAlert)) {
                updatedAlerts.add(freshAlert);
            }
        }

        return updatedAlerts;
    }

    /**
     * Get a list of stale (cancelled) alerts for a route.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of stale route alerts.
     */
    @Nonnull
    private static List<Alert> getStaleAlerts(List<Alert> existingAlerts, List<Alert> freshAlerts, Set<Alert> ignoredAlerts) {
        List<Alert> staleAlerts = new ArrayList<>();

        // If there are no fresh alerts, mark all existing as stale.
        if (freshAlerts == null || freshAlerts.isEmpty()) {
            return existingAlerts;
        }

        // Iterate through and add each staleAlert.
        for (Alert existingAlert : existingAlerts) {
            Alert existingAlertNoLocations = new Alert();
            existingAlertNoLocations.locations = new ArrayList<>();
            existingAlertNoLocations.messageTitle = existingAlert.messageTitle;
            existingAlertNoLocations.messageSubtitle = existingAlert.messageSubtitle;
            existingAlertNoLocations.messageBody = existingAlert.messageBody;
            existingAlertNoLocations.type = existingAlert.type;
            existingAlertNoLocations.route = existingAlert.route;
            existingAlertNoLocations.externalUri = existingAlert.externalUri;
            existingAlertNoLocations.highPriority = existingAlert.highPriority;
            existingAlertNoLocations.lastUpdated = existingAlert.lastUpdated;

            if (!freshAlerts.contains(existingAlert) && !ignoredAlerts.contains(existingAlertNoLocations)) {
                staleAlerts.add(existingAlert);
            }
        }

        return staleAlerts;
    }
}
