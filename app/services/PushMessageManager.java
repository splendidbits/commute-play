package services;

import com.google.inject.Inject;
import dao.AccountDao;
import dao.DeviceDao;
import enums.pushservices.Failure;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.AlertHelper;
import interfaces.pushservices.TaskQueueCallback;
import models.AgencyAlertModifications;
import models.accounts.Account;
import models.accounts.PlatformAccount;
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
import java.util.List;

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
     * @param agencyUpdates Collection of modified route alerts.
     */
    public void dispatchAlerts(@Nonnull AgencyAlertModifications agencyUpdates) {
        List<Task> taskList = new ArrayList<>();

        List<Route> updatedAlertRoutes = AlertHelper.getSortedAlertRoutes(agencyUpdates.getUpdatedAlerts());
        List<Route> staleAlertRoutes = AlertHelper.getSortedAlertRoutes(agencyUpdates.getStaleAlerts());

        // Iterate through the updated (fresh) Alert Routes to send messages for.
        for (Route updatedRoute : updatedAlertRoutes) {
            Task updatedRoutesTask = new Task(String.format("agency-%d:updated-route:%s", updatedRoute.agency != null
                    ? updatedRoute.agency.id : -1, updatedRoute.routeId));
            updatedRoutesTask.priority = Task.TASK_PRIORITY_MEDIUM;
            updatedRoutesTask.messages = createAlertMessages(agencyUpdates.getAgencyId(), updatedRoute, false);

            if (!updatedRoutesTask.messages.isEmpty()) {
                taskList.add(updatedRoutesTask);
            }
        }

        // Iterate through the stale (canceled) Alert Routes to send messages for.
        for (Route staleRoute : staleAlertRoutes) {
            Task staleRoutesTask = new Task(String.format("agency-%d:cancelled-route:%s",
                    staleRoute.agency != null ? staleRoute.agency.id : -1, staleRoute.routeId));
            staleRoutesTask.priority = Task.TASK_PRIORITY_MEDIUM;
            List<Message> messages = createAlertMessages(agencyUpdates.getAgencyId(), staleRoute, true);

            if (!messages.isEmpty()) {
                staleRoutesTask.messages = new ArrayList<>();
                staleRoutesTask.messages.addAll(messages);
                taskList.add(staleRoutesTask);
            }
        }

        // Finally, if there are messages in each task, add them to the queue.
        try {
            for (Task alertsTask : taskList) {
                mTaskQueue.queueTask(alertsTask, new SendMessagePlatformQueueCallback());
            }

        } catch (TaskValidationException e) {
            Logger.error("Commute Task threw an exception.");
        }
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
        List<Account> accounts = mAccountDao.getAccountDevices(PlatformType.SERVICE_GCM, agencyId, route.routeId);
        for (Account account : accounts) {

            // Build a new message for the platform task per API and then Platform account.
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
     * @param device   registration for newly registered device.
     * @param accounts list of the platform account owner.
     */
    public void sendRegistrationConfirmMessage(@Nonnull Device device, @Nonnull PlatformAccount accounts) {
        Task messageTask = new Task("registration-response");
        messageTask.priority = Task.TASK_PRIORITY_HIGH;
        Message message = AlertHelper.buildDeviceRegisteredMessage(device, accounts);

        if (message != null) {
            messageTask.messages.add(message);

        } else {
            Logger.error("Could not build registration confirmation message.");
        }

        // Add the message task to the TaskQueue.
        if (messageTask.messages != null) {
            try {
                mTaskQueue.queueTask(messageTask, new SendMessagePlatformQueueCallback());

            } catch (TaskValidationException e) {
                Logger.error("Commute Task threw an exception.");
            }
        }
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
