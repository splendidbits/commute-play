package services;

import com.google.inject.Inject;
import dao.AccountDao;
import dao.DeviceDao;
import enums.pushservices.FailureType;
import enums.pushservices.PlatformType;
import exceptions.pushservices.MessageValidationException;
import helpers.AlertHelper;
import interfaces.pushservices.TaskQueueListener;
import javafx.util.Pair;
import models.AlertModifications;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.Logger;
import services.pushservices.TaskQueue;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Intermediate Push Service manager for building and sending alert messages via
 * the platform push services such as APNS or GCM.
 */
public class PushMessageManager {
    private AccountDao mAccountDao;
    private DeviceDao mDeviceDao;
    private TaskQueue mTaskQueue;

    @Inject
    public PushMessageManager(AccountDao accountDao, DeviceDao deviceDao, TaskQueue taskQueue) {
        mAccountDao = accountDao;
        mDeviceDao = deviceDao;
        mTaskQueue = taskQueue;
    }

    /**
     * Notify Push subscribers of the agency alerts that have changed.
     *
     * @param alertModifications Collection of modified route alerts.
     * @return updated and cancelled alerts messages.
     */
    public Pair<Set<Message>, Set<Message>> dispatchAlerts(@Nonnull AlertModifications alertModifications) {
        Set<Message> updatedAlertMessages = new HashSet<>();
        Set<Message> staleAlertMessages = new HashSet<>();

        Map<String, List<Alert>> updatedRouteAlerts = new HashMap<>();
        Map<String, List<Alert>> staleRouteAlerts = new HashMap<>();

        /*
         * Create a list of messages for cancelled (stale) alerts.
         */
        for (Alert staleAlert : alertModifications.getStaleAlerts()) {
            Route route = staleAlert.route;
            String routeId = route.routeId;

            if (routeId == null) {
                Logger.error("Updated Alert must have routes with routeIds.");
                return null;
            }

            List<Alert> alerts = staleRouteAlerts.containsKey(routeId)
                    ? staleRouteAlerts.get(routeId)
                    : new ArrayList<>();

            alerts.add(staleAlert);
            staleRouteAlerts.put(routeId, alerts);
        }

        /*
         * Create a list of messages for updated (fresh) alerts.
         */
        for (Alert updatedAlert : alertModifications.getUpdatedAlerts()) {
            Route route = updatedAlert.route;
            String routeId = route.routeId;

            if (routeId == null) {
                Logger.error("Updated Alert must have routes with routeIds.");
                return null;
            }

            List<Alert> alerts = updatedRouteAlerts.containsKey(routeId)
                    ? updatedRouteAlerts.get(routeId)
                    : new ArrayList<>();

            alerts.add(updatedAlert);
            updatedRouteAlerts.put(routeId, alerts);
        }

        // Iterate through the updated (fresh) Alerts to send messages for.
        for (Map.Entry<String, List<Alert>> updatedAlertEntry : updatedRouteAlerts.entrySet()) {
            String routeId = updatedAlertEntry.getKey();
            List<Alert> alerts = updatedAlertEntry.getValue();

            updatedAlertMessages.addAll(createAlertMessages(alertModifications.getAgencyId(), routeId, alerts, false));
        }

        // Iterate through the removed (stale) Alerts to send messages for.
        for (Map.Entry<String, List<Alert>> staleAlertEntry : staleRouteAlerts.entrySet()) {
            String routeId = staleAlertEntry.getKey();
            List<Alert> alerts = staleAlertEntry.getValue();

            staleAlertMessages.addAll(createAlertMessages(alertModifications.getAgencyId(), routeId, alerts, true));
        }

        MessageTaskQueueListener taskQueueListener = new MessageTaskQueueListener();

        try {
            Logger.info(String.format("Sending %d Agency update alert messages to push-services module", updatedAlertMessages.size()));
            mTaskQueue.queueMessages(new ArrayList<>(updatedAlertMessages), taskQueueListener);

            Logger.info(String.format("Sending %d Agency stale alert messages to push-services module", staleAlertMessages.size()));
            mTaskQueue.queueMessages(new ArrayList<>(staleAlertMessages), taskQueueListener);


        } catch (MessageValidationException e) {
            Logger.error(String.format("Commute Task threw an exception: %s", e.getMessage()));
            return null;
        }

        return new Pair<>(updatedAlertMessages, staleAlertMessages);
    }

    /**
     * Send {@link models.alerts.Agency} route(s) alert update or cancellations flags to
     * subscribed clients by fetching the subscriptions, devices, and sender API accounts.
     * <p>
     * Creates a list of separate messages for every {@link PlatformAccount} in every {@link Account}.
     *
     * @param agencyId       The agencyId for the route.
     * @param routeId        Route ID to fetch sending accounts for.
     * @param alerts         Alert with Route for messages to send.
     * @param isCancellation set whether the alert message is an update or cancellation (clear).
     * @return A list of push service {@link Message}s to send.
     */
    @Nonnull
    private List<Message> createAlertMessages(int agencyId, @Nonnull String routeId, @Nonnull List<Alert> alerts, boolean isCancellation) {
        List<Message> messages = new ArrayList<>();
        List<Account> accounts = mAccountDao.getAccounts(PlatformType.SERVICE_GCM, agencyId, routeId);

        // Build a new message for the platform task per API and then Platform account.
        for (Account account : accounts) {
            if (account.devices != null
                    && account.platformAccounts != null
                    && !account.devices.isEmpty()
                    && !account.platformAccounts.isEmpty()) {

                // Create a message for each new alert in the route.
                for (PlatformAccount platformAccount : account.platformAccounts) {
                    for (Alert alert : alerts) {
                        List<Message> alertMessages = AlertHelper.getAlertMessages(alert, account.devices, platformAccount, isCancellation);
                        messages.addAll(alertMessages);
                    }
                }
            }
        }
        return messages;
    }

    /**
     * Used to send confirmations for successful client devices.
     * Completes the device <-> C2DM Loop.
     *
     * @param device  registration for newly registered device.
     * @param account platform account to send message to.
     * @return boolean true if there were errors dispatching the registration message.
     */
    public boolean sendRegistrationConfirmMessage(@Nonnull Device device, @Nonnull PlatformAccount account) {
        Message message = AlertHelper.buildDeviceRegisteredMessage(device, account);

        // Add the message task to the TaskQueue.
        if (message != null) {
            try {
                Logger.info(String.format("Sending confirmation message for device %s to push-services module", device.deviceId));
                mTaskQueue.queueMessages(Collections.singletonList(message), new MessageTaskQueueListener());
                return true;

            } catch (MessageValidationException e) {
                Logger.error(String.format("Commute Task threw an exception: %s", e.getMessage()));
            }
        }
        return false;
    }

    /*
     * TaskQueue Task Result Callbacks from the platform provider(s).
     */
    private class MessageTaskQueueListener implements TaskQueueListener {
        @Override
        public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRecipients) {
            Logger.info(String.format("%d recipients require registration updates.", updatedRecipients.size()));

            for (UpdatedRecipient recipientUpdate : updatedRecipients) {
                Recipient staleRecipient = recipientUpdate.getStaleRecipient();
                Recipient updatedRecipient = recipientUpdate.getUpdatedRecipient();

                mDeviceDao.saveUpdatedToken(staleRecipient.getToken(), updatedRecipient.getToken());
            }
        }

        @Override
        public void failedRecipients(@Nonnull List<Recipient> failedRecipients) {
            Logger.warn(String.format("%d recipients failed fatally.", failedRecipients.size()));

            for (Recipient recipient : failedRecipients) {
                FailureType failure = recipient.getPlatformFailure().getFailureType();

                if (failure != null && (failure == FailureType.RECIPIENT_REGISTRATION_INVALID ||
                                failure == FailureType.RECIPIENT_NOT_REGISTERED ||
                                failure == FailureType.MESSAGE_PACKAGE_INVALID)) {
                    Logger.error(String.format("GCM Failure: Deleted recipient %s", recipient.getToken()));
                    mDeviceDao.removeDevice(recipient.getToken());
                }
            }
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {
            Logger.info(String.format("Message %d completed.", originalMessage.getId()));
        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
            Logger.info(String.format("Message %d failed - %s.", originalMessage.getId(), failure.getFailureMessage()));
        }
    }

}
