package helpers;

import enums.AlertType;
import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import helpers.pushservices.PlatformMessageBuilder;
import models.AgencyAlertModifications;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.PayloadElement;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import services.fluffylog.Logger;

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
     * Get a list of Agency Routes for the given alerts, with the alerts added to their respective routes.
     *
     * @param alerts list of alerts.
     * @return list of unique routes.
     */
    public static List<Route> getSortedAlertRoutes(List<Alert> alerts) {
        List<Route> routes = new ArrayList<>();
        if (alerts != null) {

            for (Alert alert : alerts) {
                if (alert.route != null && alert.route.routeId != null && !routes.contains(alert.route)) {
                    alert.route.subscriptions = new ArrayList<>();
                    alert.route.alerts = new ArrayList<>();
                    routes.add(alert.route);
                }
            }

            for (Route route : routes) {
                for (Alert alert : alerts) {
                    if (alert.route.routeId.equals(route.routeId) && route.alerts != null) {
                        route.alerts.add(alert);
                        alert.route = route;
                    }
                }
            }
        }
        return routes;
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param existingAgency the currently saved agency.
     * @param agency  the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    @Nullable
    public static AgencyAlertModifications getAgencyModifications(@Nullable Agency existingAgency, @Nullable Agency agency) {

        // Both agencies do not exist. This is bad.
        if (existingAgency == null && agency == null) {
            Logger.warn("Both agencies for modifications calculation were null!");
            return null;
        }

        // Copy the routes.
        List<Route> existingRoutes = existingAgency != null ? copyRoute(existingAgency.routes) : null;
        List<Route> freshRoutes = agency != null ? copyRoute(agency.routes) : null;

        int agencyId = agency != null ? agency.id : existingAgency.id;
        AgencyAlertModifications alertModifications = new AgencyAlertModifications(agencyId);

        // Updated agency exists and Existing agency does not.
        if ((existingRoutes == null || existingRoutes.isEmpty()) && freshRoutes != null) {
            Logger.info(String.format("Existing routes for agency %d missing. Marking all as updated.", agencyId));

            for (Route freshRoute : freshRoutes) {
                alertModifications.addUpdatedAlerts(freshRoute.alerts);
            }
            return alertModifications;
        }

        // Existing agency exists and Updated agency does not.
        if ((freshRoutes == null || freshRoutes.isEmpty()) && existingRoutes != null) {
            Logger.info(String.format("New routes for agency %d missing. Marking all as stale.", agencyId));

            for (Route staleRoute : existingRoutes) {
                alertModifications.addUpdatedAlerts(staleRoute.alerts);
            }
            return alertModifications;
        }

        List<Alert> updatedAlerts = new ArrayList<>();
        List<Alert> staleAlerts = new ArrayList<>();

        // Iterate through the existing routes and check existing and fresh Routes are the same.
        if (freshRoutes != null) {
            for (Route freshRoute : freshRoutes) {
                for (Route existingRoute : existingRoutes) {

                    if (freshRoute.routeId.equals(existingRoute.routeId)) {
                        updatedAlerts.addAll(getUpdatedAlerts(existingRoute, existingRoute.alerts, freshRoute.alerts));
                        staleAlerts.addAll(getStaleAlerts(existingRoute, existingRoute.alerts, freshRoute.alerts));

                        // There was a route match so skip the inner loop.
                        break;
                    }
                }
            }
        }

        // Remove any alert from the stale alerts where the route type was updated.
        Iterator<Alert> staleAlertsIterator = staleAlerts.iterator();
        while (staleAlertsIterator.hasNext()) {
            Alert staleAlert = staleAlertsIterator.next();

            // If the stale alert routeId is the same as the updated alert, remove the stale alert.
            for (Alert updatedAlert : updatedAlerts) {
                boolean bothRouteIdsExist = updatedAlert.route != null && updatedAlert.route.routeId != null &&
                        staleAlert.route != null && staleAlert.route.routeId != null;

                if (bothRouteIdsExist && updatedAlert.route.routeId.equals(staleAlert.route.routeId)) {
                    staleAlertsIterator.remove();
                    break;
                }
            }
        }

        alertModifications.addUpdatedAlerts(updatedAlerts);
        alertModifications.addStaleAlerts(staleAlerts);
        return alertModifications;
    }

    /**
     * Get a list of fresh (new) alerts that do not exist in the previous (existing) collection.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of updated route alerts.
     */
    @Nonnull
    private static List<Alert> getUpdatedAlerts(Route route, List<Alert> existingAlerts, List<Alert> freshAlerts) {
        List<Alert> updatedAlerts = new ArrayList<>();

        if (freshAlerts != null) {
            for (Alert freshAlert : freshAlerts) {

                if (!AlertHelper.isAlertEmpty(freshAlert) &&
                        (existingAlerts == null || existingAlerts.isEmpty() || !existingAlerts.contains(freshAlert))) {
                    Logger.info(String.format("Alert in route %1$s was new or updated.", route.routeId));
                    freshAlert.route = route;
                    updatedAlerts.add(freshAlert);
                }
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
    private static List<Alert> getStaleAlerts(Route route, List<Alert> existingAlerts, List<Alert> freshAlerts) {
        List<Alert> staleAlerts = new ArrayList<>();

        // Sanity check for both lists being empty.
        if ((existingAlerts == null || existingAlerts.isEmpty()) && (freshAlerts == null || freshAlerts.isEmpty())) {
            return staleAlerts;
        }

        // Mark all existing alerts as stale if fresh alerts are empty.
        if (freshAlerts == null || freshAlerts.isEmpty() && existingAlerts != null && !existingAlerts.isEmpty()) {
            return existingAlerts;
        }

        // Sanity check the fresh alert is empty, use this iteration to add to stale alerts.
        if (AlertHelper.areAlertsEmpty(freshAlerts) && (existingAlerts != null && !existingAlerts.isEmpty())) {
            return existingAlerts;
        }

        if (existingAlerts != null) {
            Set<AlertType> freshAlertTypes = new HashSet<>();

            // Add all fresh alert types to a set.
            for (Alert freshAlert : freshAlerts) {
                freshAlertTypes.add(freshAlert.type);
            }

            // If an existing alert type does not exist in the updated fresh alerts, it is stale.
            for (Alert existingAlert : existingAlerts) {
                if (!freshAlertTypes.contains(existingAlert.type)) {
                    Logger.info(String.format("Alert in route %s became stale.", route.routeId));
                    staleAlerts.add(existingAlert);
                }
            }
        }
        return staleAlerts;
    }

    /**
     * Check if the Alert is "empty" (if there is no message, or type.
     *
     * @param alert the alert to check.
     * @return true if the message is empty.
     */
    private static boolean isAlertEmpty(@Nonnull Alert alert) {
        boolean messageBodyEmpty = alert.messageBody == null || alert.messageBody.isEmpty();
        boolean messageTitleEmpty = alert.messageTitle == null || alert.messageTitle.isEmpty();
        boolean messageTypeNone = alert.type == null || AlertType.TYPE_NONE.equals(alert.type);

        return (messageBodyEmpty || messageTitleEmpty) || messageTypeNone;
    }

    /**
     * Returns true if all alerts are empty.
     *
     * @param alerts list of alerts to check.
     * @return true if all alerts in collection are empty.
     */
    private static boolean areAlertsEmpty(@Nonnull List<Alert> alerts) {
        for (Alert alert : alerts) {
            if (!isAlertEmpty(alert)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deep copy a list of Routes including all of its children.
     *
     * @param routes {@link List<Route>} list to copy.
     * @return copied {@link List<Route>}.
     */
    private static List<Route> copyRoute(List<Route> routes) {
        List<Route> copiedRoutes = new ArrayList<>();
        if (routes != null) {
            for (Route route : routes) {
                copiedRoutes.add(copyRoute(route));
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
    private static Route copyRoute(@Nonnull Route route) {
        try {
            return (Route) route.clone();

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
