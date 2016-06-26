package services;

import agency.AgencyModifications;
import com.google.inject.Inject;
import dao.AccountsDao;
import dao.DeviceDao;
import enums.pushservices.PlatformType;
import enums.pushservices.RecipientState;
import exceptions.pushservices.TaskValidationException;
import helpers.CommuteAlertHelper;
import interfaces.pushservices.TaskQueueCallback;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Route;
import models.devices.Device;
import models.pushservices.Message;
import models.pushservices.PlatformFailure;
import models.pushservices.Recipient;
import models.pushservices.Task;
import services.pushservices.TaskQueue;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Intermediate Push Service manager for building and sending alert messages via
 * the platform push services such as APNS or GCM.
 */
public class PushMessageManager {
    private AccountsDao mAccountsDao;
    private DeviceDao mDeviceDao;
    private TaskQueue mTaskQueue;

    @Inject
    public PushMessageManager(AccountsDao accountsDao, DeviceDao deviceDao, TaskQueue taskQueue) {
        mAccountsDao = accountsDao;
        mDeviceDao = deviceDao;
        mTaskQueue = taskQueue;
    }

    /**
     * Notify Push subscribers of the agency alerts that have changed.
     *
     * @param agencyUpdates Collection of modified route alerts.
     */
    public CompletableFuture<Boolean> dispatchAlerts(@Nonnull AgencyModifications agencyUpdates) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        List<Task> taskList = new ArrayList<>();

        // Iterate through the new Alerts to send.
        for (Route updatedRoute : agencyUpdates.getUpdatedAlertRoutes()) {
            Task updatedRoutesTask = new Task(String.format("agency-%d:updated-route:%s",
                    updatedRoute.agency != null ? updatedRoute.agency.id : -1, updatedRoute.routeId));
            updatedRoutesTask.priority = Task.TASK_PRIORITY_MEDIUM;

            List<Message> messages = createAlertGcmMessages(agencyUpdates.getAgencyId(), updatedRoute);
            if (!messages.isEmpty()) {
                updatedRoutesTask.messages = new ArrayList<>();
                updatedRoutesTask.messages.addAll(messages);
                taskList.add(updatedRoutesTask);
            }
        }

        // Iterate through the stale (canceled) Alerts to send.
        for (Route staleRoute : agencyUpdates.getStaleAlertRoutes()) {
            Task staleRoutesTask = new Task(String.format("agency-%d:cancelled-route:%s",
                    staleRoute.agency != null ? staleRoute.agency.id : -1, staleRoute.routeId));
            staleRoutesTask.priority = Task.TASK_PRIORITY_LOW;

            List<Message> messages = createAlertGcmMessages(agencyUpdates.getAgencyId(), staleRoute);
            if (!messages.isEmpty()) {
                staleRoutesTask.messages = new ArrayList<>();
                staleRoutesTask.messages.addAll(messages);
                taskList.add(staleRoutesTask);
            }
        }

        // Finally, if there are messages in each task, add them to the queue.
        try {
            for (Task alertsTask : taskList) {
                mTaskQueue.queueTask(alertsTask, new SendMessagePlatformQueueCallback(completableFuture));
            }

        } catch (TaskValidationException e) {
            Logger.error("Commute Task threw an exception.");
            CompletableFuture.completedFuture(false);
        }

        return completableFuture;
    }

    /**
     * Send {@link models.alerts.Agency} route(s) alert update or cancellations flags to
     * subscribed clients by fetching the subscriptions, devices, and sender API accounts.
     * <p>
     * Creates a list of separate messages for every {@link PlatformAccount} in every {@link Account}.
     *
     * @param agencyId The agencyId for the route.
     * @param route    A route which contains cancelled route alerts to dispatch.
     * @return A list of push service {@link Message}s to send.
     */
    @Nonnull
    private List<Message> createAlertGcmMessages(int agencyId, @Nonnull Route route) {
        List<Message> messages = new ArrayList<>();

        // Get all accounts with devices subscribed to that route.
        List<Account> accounts = mAccountsDao.getAccountDevices(PlatformType.SERVICE_GCM, agencyId, route);

        // Iterate through each sending API account.
        if (accounts != null && route.alerts != null) {
            for (Account account : accounts) {

                // Build a new message for the platform task per API and then Platform account.
                if (account.devices != null && account.platformAccounts != null && !account.devices.isEmpty() &&
                        !account.platformAccounts.isEmpty()) {

                    // Create a message for each new alert in the route.
                    for (PlatformAccount platformAccount : account.platformAccounts) {
                        List<Message> alertMessages = CommuteAlertHelper.getAlertMessages(route, account.devices,
                                platformAccount);
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
     * @param device   registration for newly registered device.
     * @param accounts list of the platform account owner.
     */
    public CompletionStage<Boolean> sendRegistrationConfirmMessage(@Nonnull Device device,
                                                                   @Nonnull PlatformAccount accounts) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        Task messageTask = new Task("registration-response");
        messageTask.priority = Task.TASK_PRIORITY_HIGH;
        Message message = CommuteAlertHelper.buildDeviceRegisteredMessage(device, accounts);

        if (message != null) {
            messageTask.messages.add(message);

        } else {
            Logger.warn("Could not build registration confirmation message.");
            CompletableFuture.completedFuture(false);
        }

        // Add the message task to the TaskQueue.
        if (messageTask.messages != null) {
            try {
                mTaskQueue.queueTask(messageTask, new SendMessagePlatformQueueCallback(completableFuture));

            } catch (TaskValidationException e) {
                Logger.error("Commute Task threw an exception.");
                CompletableFuture.completedFuture(false);
            }
        }
        return completableFuture;
    }

    /**
     * TaskQueue Task Result Callbacks from the platform provider(s).
     */
    private class SendMessagePlatformQueueCallback implements TaskQueueCallback {
        private CompletableFuture<Boolean> mCompletableFuture;

        SendMessagePlatformQueueCallback(CompletableFuture<Boolean> completableFuture) {
            mCompletableFuture = completableFuture;
        }

        @Override
        public void updatedRecipient(@Nonnull Map<Recipient, Recipient> updatedRegistrations) {
            Logger.info(String.format("%d recipients require registration updates.", updatedRegistrations.size()));
        }

        @Override
        public void invalidRecipients(@Nonnull Set<Recipient> recipients) {
            Logger.warn(String.format("%d recipients need to be deleted.", recipients.size()));
            for (Recipient recipientToRemove : recipients) {
                mDeviceDao.removeDevice(recipientToRemove.token);
            }
        }

        @Override
        public void failedRecipient(@Nonnull Recipient failedRecipient, @Nonnull PlatformFailure failure) {
            Logger.warn(String.format("Recipient %d failed - %s.", failedRecipient.id, failure.failureMessage));
        }

        @Override
        public void messageCompleted(@Nonnull Message originalMessage) {
            Logger.debug(String.format("Message %d completed.", originalMessage.id));

            // Sanity check that all recipients are completed or failed.
            for (Recipient recipient : originalMessage.recipients) {
                if (!(recipient.state == RecipientState.STATE_COMPLETE ||
                        recipient.state == RecipientState.STATE_FAILED)) {
                    throw new RuntimeException("messageCompleted() response did not match recipient states.");
                }
            }
        }

        @Override
        public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
            Logger.debug(String.format("Message %d failed - %s.", originalMessage.id, failure.failureMessage));
            mCompletableFuture.complete(false);

            // Sanity check that all recipients are failed.
            for (Recipient recipient : originalMessage.recipients) {
                if (recipient.state != RecipientState.STATE_FAILED) {
                    throw new RuntimeException("messageFailed() response did not match recipient states.");
                }
            }
        }
    }
}
