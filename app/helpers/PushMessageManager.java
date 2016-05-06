package helpers;

import agency.AgencyModifications;
import pushservices.interfaces.TaskMessageResponse;
import pushservices.models.app.UpdatedRegistration;
import pushservices.services.TaskQueue;
import pushservices.types.PushFailCause;
import pushservices.enums.PlatformType;
import services.splendidlog.Log;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.alerts.Route;
import models.devices.Device;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;
import org.jetbrains.annotations.NotNull;
import services.AccountsDao;
import services.DeviceDao;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static helpers.CommuteGcmBuilder.buildPushMessage;

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
                List<Task> messageTasks = new ArrayList<>();

                // Iterate through the NEW Alerts to send.
                for (Route updatedRoute : agencyUpdates.getUpdatedAlertRoutes()) {
                    messageTasks.addAll(createAlertUpdateMessages(updatedRoute, agencyUpdates.getAgencyId()));
                }

                // Iterate through the STALE (canceled) Alerts to send.
                for (Route staleRoute: agencyUpdates.getStaleAlertRoutes()) {
                    messageTasks.addAll(createAlertCancelMessages(staleRoute, agencyUpdates.getAgencyId()));
                }

                // Finally, if there are messages in each task, add them to the queue.
                for (Task outboundTask : messageTasks) {
                    mTaskQueue.addTask(outboundTask, new SendAlertRecipientResponseTask());
                    completableFuture.complete(true);
                }
            }
        });

        return completableFuture;
    }

    /**
     * Send updated {@link models.alerts.Agency} route alerts to subscribed clients
     * by fetching the subscriptions, devices, and the sender API accost.
     *
     * @param updatedRoute A route which contains updated alerts to dispatch.
     * @param agencyId The agencyId for the route
     *
     * @return A list of push service {@link Task}s to send.
     */
    @Nonnull
    private List<Task> createAlertUpdateMessages(@Nonnull Route updatedRoute, int agencyId) {
        List<Task> tasksList = new ArrayList<>();

        // Get all accounts with devices subscribed to that route.
        List<Account> accounts = mAccountsDao.getAccounts(PlatformType.SERVICE_GCM, agencyId, updatedRoute);

        // Iterate through each sending API account.
        if (accounts != null && !accounts.isEmpty() &&
                updatedRoute.alerts != null && !updatedRoute.alerts.isEmpty()) {
            Task task = new Task(updatedRoute.routeId);

            // Build a new message for the platform task per API account.
            for (Account account : accounts) {
                for (Alert alert : updatedRoute.alerts) {

                    if (account.devices != null && account.platformAccounts != null) {
                        Message message = buildPushMessage(alert, false, account.devices,
                                account.platformAccounts);

                        if (message != null) {
                            task.addMessage(message);

                        } else {
                            mLog.w(TAG, "Could not build alert update message.");
                            CompletableFuture.completedFuture(false);
                        }
                    }
                }
            }
            // Add the task to the list of outbound jobs.
            tasksList.add(task);
        }
        return tasksList;
    }

    /**
     * Send {@link models.alerts.Agency} route(s) notification cancellations flags to
     * subscribed clients by fetching the subscriptions, devices, and
     * the sender API accost.
     *
     * @param cancelledRoute A route which contains cancelled route alerts to dispatch.
     * @param agencyId The agencyId for the route
     *
     * @return A list of push service {@link Task}s to send.
     */
    @Nonnull
    private List<Task> createAlertCancelMessages(@Nonnull Route cancelledRoute, int agencyId) {
        List<Task> tasksList = new ArrayList<>();

        // Get all accounts with devices subscribed to that route.
        List<Account> accounts = mAccountsDao.getAccounts(PlatformType.SERVICE_GCM, agencyId, cancelledRoute);

        // Iterate through each sending API account.
        if (accounts != null && !accounts.isEmpty() &&
                cancelledRoute.alerts != null && !cancelledRoute.alerts.isEmpty()) {
            final String taskName = cancelledRoute.routeId + "_cancel";
            Task task = new Task(taskName);

            // Build a new message for the platform task per API account.
            for (Account account : accounts) {
                for (Alert alert : cancelledRoute.alerts) {
                    if (account.devices != null && account.platformAccounts != null) {
                        Message message = buildPushMessage(alert, true, account.devices,
                                account.platformAccounts);

                        if (message != null) {
                            task.addMessage(message);
                        } else {
                            mLog.w(TAG, "Could not build alert cancel message.");
                            CompletableFuture.completedFuture(false);
                        }
                    }
                }
            }
            // Add the task to the list of outbound jobs.
            tasksList.add(task);
        }
        return tasksList;
    }

    /**
     * Used to send confirmations for successful client devices.
     * Completes the device <-> C2DM Loop.
     *
     * @param device     registration for newly registered device.
     * @param platformAccounts list of the platform account owner.
     */
    public CompletionStage<Boolean> sendRegistrationConfirmation(@Nonnull Device device,
                                                                 @Nonnull List<PlatformAccount> platformAccounts) {
        if (!platformAccounts.isEmpty()) {
            // Create a new task and send a message per platform account.
            Task messageTask = new Task("registration_success");

            Message message = CommuteGcmBuilder.buildConfirmDeviceMessage(device, platformAccounts);
            if (message != null) {
                message.addRecipient(new Recipient(device.token));
                messageTask.addMessage(message);

            } else {
                mLog.w(TAG, "Could not build registration confirmation message.");
                CompletableFuture.completedFuture(false);
            }

            // Add the message task to the TaskQueue.
            if (messageTask.messages != null) {
                mTaskQueue.addTask(messageTask, new SendAlertRecipientResponseTask());
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Result Callbacks from the pushservices.
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
