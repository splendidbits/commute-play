package services;

import com.google.inject.Inject;
import dao.AccountDao;
import dao.DeviceDao;
import enums.pushservices.Failure;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.AlertHelper;
import interfaces.pushservices.TaskQueueCallback;
import models.AlertModifications;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import models.pushservices.db.Task;
import services.fluffylog.Logger;
import services.pushservices.TaskQueue;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @return boolean true if there were errors dispatching the modifications.
     */
    public boolean dispatchAlerts(@Nonnull AlertModifications alertModifications) {
        List<Task> taskList = new ArrayList<>();

        /*
         * Iterate through each updated and stale alert and combine them into groups of
         * their respective routes.
         *
         * For example, if 3 alerts have the route_id "bus_route_42", ensure the Route is added
         * to the updated list, and that it has all 3 updated alerts. Do the same for stale alerts.
         */
        Map<String, Route> updatedRoutes = new HashMap<>();
        for (Alert updatedAlert : alertModifications.getUpdatedAlerts()) {
            if (updatedAlert.route == null) {
                Logger.error("Updated Alert must have routes attached.");
                return false;
            }

            if (updatedAlert.route.alerts == null) {
                Logger.error("Route for Updated Alert must have all Alert back-references.");
                return false;
            }

            updatedRoutes.put(updatedAlert.route.routeId, updatedAlert.route);
        }

        Map<String, Route> staleRoutes = new HashMap<>();
        for (Alert staleAlert : alertModifications.getStaleAlerts()) {
            if (staleAlert.route == null) {
                Logger.error("Stale Alert must have routes attached.");
            }

            if (staleAlert.route.alerts == null) {
                Logger.error("Route for Stale Alert must have all Alert back-references.");
                return false;
            }

            staleRoutes.put(staleAlert.route.routeId, staleAlert.route);
        }

        // Iterate through the updated (fresh) Alerts to send messages for.
        for (Route updatedRoute : updatedRoutes.values()) {
            Task updatedRouteTask = new Task(String.format("agency-%d:updated-route:%s", updatedRoute.agency != null
                    ? updatedRoute.agency.id
                    : -1, updatedRoute.routeId));

            updatedRouteTask.priority = Task.TASK_PRIORITY_MEDIUM;
            updatedRouteTask.messages = createAlertMessages(alertModifications.getAgencyId(), updatedRoute, false);

            if (!updatedRouteTask.messages.isEmpty()) {
                taskList.add(updatedRouteTask);
            } else {
                Logger.error("No messages were generated for updated route");
                return false;
            }
        }

        // Iterate through the stale (expunged) Alerts to send messages for.
        for (Route staleRoute : staleRoutes.values()) {
            Task staleRouteTask = new Task(String.format("agency-%d:stale-route:%s", staleRoute.agency != null
                    ? staleRoute.agency.id
                    : -1, staleRoute.routeId));

            staleRouteTask.priority = Task.TASK_PRIORITY_MEDIUM;
            staleRouteTask.messages = createAlertMessages(alertModifications.getAgencyId(), staleRoute, true);

            if (!staleRouteTask.messages.isEmpty()) {
                taskList.add(staleRouteTask);
            } else {
                Logger.error("No messages were generated for stale route");
                return false;
            }
        }

        try {
            // Add each task to the queue.
            for (Task alertsTask : taskList) {
                mTaskQueue.queueTask(alertsTask, new SendMessagePlatformQueueCallback());
            }

        } catch (TaskValidationException e) {
            Logger.error("Commute Task threw an exception.");
            return false;
        }

        return !taskList.isEmpty();
    }

    /**
     * Send {@link models.alerts.Agency} route(s) alert update or cancellations flags to
     * subscribed clients by fetching the subscriptions, devices, and sender API accounts.
     * <p>
     * Creates a list of separate messages for every {@link PlatformAccount} in every {@link Account}.
     *
     * @param agencyId       The agencyId for the route.
     * @param route          Route with messages to send.
     * @param isCancellation set whether the alert message is an update or cancellation (clear).
     * @return A list of push service {@link Message}s to send.
     */
    @Nonnull
    private List<Message> createAlertMessages(int agencyId, @Nonnull Route route, boolean isCancellation) {
        if (route.alerts == null) {
            throw new RuntimeException("Route does not contain any alerts.");
        }

        List<Message> messages = new ArrayList<>();
        List<Account> accounts = mAccountDao.getAccounts(PlatformType.SERVICE_GCM, agencyId, route.routeId);

        // Build a new message for the platform task per API and then Platform account.
        for (Account account : accounts) {
            if (account.devices != null
                    && account.platformAccounts != null
                    && !account.devices.isEmpty()
                    && !account.platformAccounts.isEmpty()) {

                // Create a message for each new alert in the route.
                for (PlatformAccount platformAccount : account.platformAccounts) {
                    List<Message> alertMessages = AlertHelper.getAlertMessages(
                            route,
                            account.devices,
                            platformAccount,
                            isCancellation);
                    messages.addAll(alertMessages);
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
        Task messageTask = new Task("registration-response");
        messageTask.priority = Task.TASK_PRIORITY_HIGH;
        Message message = AlertHelper.buildDeviceRegisteredMessage(device, account);

        if (message != null) {
            messageTask.messages.add(message);

        } else {
            Logger.error("Could not build registration confirmation message.");
            return false;
        }

        // Add the message task to the TaskQueue.
        if (messageTask.messages != null) {
            try {
                mTaskQueue.queueTask(messageTask, new SendMessagePlatformQueueCallback());

            } catch (TaskValidationException e) {
                Logger.error("Commute Task threw an exception.");
                return false;
            }
        }

        return messageTask.messages != null && !messageTask.messages.isEmpty();
    }

    /**
     * TaskQueue Task Result Callbacks from the platform provider(s).
     */
    private class SendMessagePlatformQueueCallback implements TaskQueueCallback {

        @Override
        public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRecipients) {
            Logger.info(String.format("%d recipients require registration updates.", updatedRecipients.size()));

            for (UpdatedRecipient recipientUpdate : updatedRecipients) {
                Recipient staleRecipient = recipientUpdate.getStaleRecipient();
                Recipient updatedRecipient = recipientUpdate.getUpdatedRecipient();

                mDeviceDao.saveUpdatedToken(staleRecipient.token, updatedRecipient.token);
            }
        }

        @Override
        public void failedRecipients(@Nonnull List<FailedRecipient> failedRecipients) {
            Logger.warn(String.format("%d recipients failed fatally.", failedRecipients.size()));

            for (FailedRecipient recipientFailure : failedRecipients) {
                Recipient failedRecipient = recipientFailure.getRecipient();
                PlatformFailure failure = recipientFailure.getFailure();

                if (failure.failure != null && (failure.failure == Failure.RECIPIENT_REGISTRATION_INVALID ||
                        failure.failure == Failure.RECIPIENT_NOT_REGISTERED ||
                        failure.failure == Failure.MESSAGE_PACKAGE_INVALID)) {
                    mDeviceDao.removeDevice(failedRecipient.token);
                }
            }
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {
            Logger.debug(String.format("Message %d completed.", originalMessage.id));
        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
            Logger.debug(String.format("Message %d failed - %s.", originalMessage.id, failure.failureMessage));
        }
    }

}
