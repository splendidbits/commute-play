package services;

import agency.AgencyModifications;
import helpers.CommuteAlertHelper;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Route;
import models.devices.Device;
import org.jetbrains.annotations.NotNull;
import pushservices.enums.PlatformType;
import pushservices.interfaces.TaskMessageResponse;
import pushservices.models.app.UpdatedRegistration;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;
import pushservices.services.TaskQueue;
import pushservices.types.PushFailCause;
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Intermediate Push Service manager for building and sending alert messages via
 * the platform push services such as APNS or GCM.
 */
public class PushMessageManager {
    private static final String TAG = PushMessageManager.class.getSimpleName();

    @Inject
    private AccountsDao mAccountsDao;

    @Inject
    private DeviceDao mDeviceDao;

    @Inject
    private Log mLog;

    @Inject
    private TaskQueue mTaskQueue;

    /**
     * Notify Push subscribers of the agency alerts that have changed.
     *
     * @param agencyUpdates Collection of modified route alerts.
     */
    public CompletionStage<Boolean> dispatchAlerts(@Nonnull AgencyModifications agencyUpdates) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(new Runnable() {

            @Override
            public void run() {
                List<Task> taskList = new ArrayList<>();

                // Iterate through the NEW Alerts to send.
                for (Route updatedRoute : agencyUpdates.getUpdatedAlertRoutes()) {
                    Task updatedRoutesTask = new Task(String.format("agency-%d_updated_route-%s",
                            updatedRoute.agency != null ? updatedRoute.agency.id : -1, updatedRoute.routeId));
                    updatedRoutesTask.priority = Task.TASK_PRIORITY_MEDIUM;

                    List<Message> messages = createAlertGcmMessages(agencyUpdates.getAgencyId(), updatedRoute, false);
                    if (!messages.isEmpty()) {
                        updatedRoutesTask.messages = new ArrayList<>();
                        updatedRoutesTask.messages.addAll(messages);
                        taskList.add(updatedRoutesTask);
                    }
                }

                // Iterate through the STALE (canceled) Alerts to send.
                for (Route staleRoute : agencyUpdates.getStaleAlertRoutes()) {
                    Task staleRoutesTask = new Task(String.format("agency-%d_cancelled_route-%s",
                            staleRoute.agency != null ? staleRoute.agency.id : -1, staleRoute.routeId));
                    staleRoutesTask.priority = Task.TASK_PRIORITY_LOW;

                    List<Message> messages = createAlertGcmMessages(agencyUpdates.getAgencyId(), staleRoute, true);
                    if (!messages.isEmpty()) {
                        staleRoutesTask.messages = new ArrayList<>();
                        staleRoutesTask.messages.addAll(messages);
                        taskList.add(staleRoutesTask);
                    }
                }

                // Finally, if there are messages in each task, add them to the queue.
                for (Task outboundTask : taskList) {
                    mTaskQueue.addTask(outboundTask, new SendAlertRecipientResponseTask());
                    completableFuture.complete(true);
                }
            }
        });

        return completableFuture;
    }

    /**
     * Send {@link models.alerts.Agency} route(s) alert update or cancellations flags to
     * subscribed clients by fetching the subscriptions, devices, and sender API accounts.
     * <p>
     * Creates a list of separate messages for every {@link PlatformAccount} in every {@link Account}.
     *
     * @param agencyId        The agencyId for the route.
     * @param route           A route which contains cancelled route alerts to dispatch.
     * @param isCancelMessage if the alert is an alert cancellation message.
     * @return A list of push service {@link Message}s to send.
     */
    @Nonnull
    private List<Message> createAlertGcmMessages(int agencyId, @Nonnull Route route, boolean isCancelMessage) {
        List<Message> messages = new ArrayList<>();

        // Get all accounts with devices subscribed to that route.
        List<Account> accounts = mAccountsDao.getAccounts(PlatformType.SERVICE_GCM, agencyId, route);

        // Iterate through each sending API account.
        if (accounts != null && route.alerts != null) {
            for (Account account : accounts) {

                // Build a new message for the platform task per API and then Platform account.
                if (account.devices == null || account.platformAccounts == null ||
                        account.devices.isEmpty() || account.platformAccounts.isEmpty()) {
                    // Continue to next account if there are 0 devices or platforms for API account..
                    continue;
                }

                // Create a message for each new alert in the route.
                for (PlatformAccount platformAccount : account.platformAccounts) {
                    List<Message> alertMessages = CommuteAlertHelper.getAlertMessages(route,
                            account.devices, platformAccount);
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
     * @param device           registration for newly registered device.
     * @param platformAccounts list of the platform account owner.
     */
    public CompletionStage<Boolean> sendRegistrationConfirmMessage(@Nonnull Device device,
                                                                   @Nonnull PlatformAccount platformAccounts) {
        Task messageTask = new Task("registration_success");
        messageTask.priority = Task.TASK_PRIORITY_HIGH;
        Message message = CommuteAlertHelper.buildDeviceRegisteredMessage(device, platformAccounts);

        if (message != null) {
            messageTask.addMessage(message);

        } else {
            mLog.w(TAG, "Could not build registration confirmation message.");
            CompletableFuture.completedFuture(false);
        }

        // Add the message task to the TaskQueue.
        if (messageTask.messages != null) {
            mTaskQueue.addTask(messageTask, new SendAlertRecipientResponseTask());
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Result Callbacks from the push-services.
     */
    private class SendAlertRecipientResponseTask implements TaskMessageResponse {

        @Override
        public void removeRecipients(@NotNull List<Recipient> recipients) {
            mLog.d(TAG, String.format("There are %d recipients to delete locally.", recipients.size()));
            for (Recipient recipientToRemove : recipients) {
                mDeviceDao.removeDevice(recipientToRemove.token);
            }
        }

        @Override
        public void updateRecipients(@NotNull List<UpdatedRegistration> recipients) {
            mLog.d(TAG, String.format("There are %d recipients to delete locally.", recipients.size()));
            for (UpdatedRegistration recipientToUpdate : recipients) {
                // TODO: Implement this.
            }
        }

        @Override
        public void completed(@Nonnull Task task) {
            mLog.d(TAG, String.format("The task %s completed successfully.", task.name));
        }

        @Override
        public void failed(@Nonnull Task task, @Nonnull PushFailCause reason) {
            mLog.d(TAG, String.format("The task %s$1 failed because of %s$2.", task.name, reason.name()));
        }

    }
}
