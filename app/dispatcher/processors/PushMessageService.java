package dispatcher.processors;

import appmodels.ModifiedAlerts;
import dispatcher.interfaces.PushMessageCallback;
import dispatcher.models.UpdatedRecipient;
import dispatcher.types.PushFailCause;
import enums.PlatformType;
import helpers.MessageHelper;
import main.Log;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.Task;
import services.AccountService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static helpers.MessageHelper.buildPushMessage;

/**
 * Application-wide service for sending information through the
 * platform push channels.
 */
public class PushMessageService {
    private static final String TAG = PushMessageService.class.getSimpleName();

    @Inject
    private AccountService mAccountService;

    @Inject
    private Log mLog;

    @Inject
    private TaskQueue mTaskQueue;

    /**
     * Notify Push subscribers of the agency alerts that have changed.
     *
     * @param agencyUpdates Collection of modified route alerts.
     */
    public CompletionStage<Boolean> notifyAlertSubscribers(@Nonnull ModifiedAlerts agencyUpdates) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(new Runnable() {

            @Override
            public void run() {
                List<Task> messageTasks = new ArrayList<>();

                /** Iterate through the NEW Alerts to send. **/
                for (Alert newAlert : agencyUpdates.getUpdatedAlerts()) {

                    // Get all accounts with registrations subscribed to that route.
                    List<Account> accounts = mAccountService.getRegistrationAccounts(
                            PlatformType.SERVICE_GCM,
                            agencyUpdates.getAgencyId(),
                            newAlert.route);

                    // Iterate through each sending API account.
                    if (accounts != null && !accounts.isEmpty()) {
                        Task task = new Task(newAlert.route.routeId);

                        // Build a new message for the platform task per API account.
                        for (Account account : accounts) {
                            Message message = buildPushMessage(newAlert, false,
                                    account.registrations, account.platformAccounts);

                            if (message != null) {
                                task.addMessage(message);
                            }
                        }
                        // Add the task to the list of outbound jobs.
                        messageTasks.add(task);
                    }
                }

                /** Iterate through the STALE (canceled) Alerts to send. **/
                for (Alert staleAlert : agencyUpdates.getUpdatedAlerts()) {

                    // Get all accounts with registrations subscribed to that route.
                    List<Account> accounts = mAccountService.getRegistrationAccounts(
                            PlatformType.SERVICE_GCM,
                            agencyUpdates.getAgencyId(),
                            staleAlert.route);

                    // Iterate through each sending API account.
                    if (accounts != null && !accounts.isEmpty()) {
                        Task task = new Task(staleAlert.route.routeId);

                        // Build a new message for the platform task per API account.
                        for (Account account : accounts) {
                            Message message = buildPushMessage(staleAlert,
                                    true, account.registrations, account.platformAccounts);

                            if (message != null) {
                                task.addMessage(message);
                            }
                        }
                        // Add the task to the list of outbound jobs.
                        messageTasks.add(task);
                    }
                }

                // Finally, if there are messages in each task, add them to the queue.
                for (Task outboundTask : messageTasks) {
                    if (outboundTask.messages != null && !outboundTask.messages.isEmpty()) {
                        mTaskQueue.addTask(outboundTask, new SendAlertRecipientCallback());
                        completableFuture.complete(true);
                    }
                }
            }
        });

        return completableFuture;
    }

    /**
     * Used to send confirmations for successful client registrations.
     * Completes the device <-> C2DM Loop.
     *
     * @param registration     registration for newly registered device.
     * @param platformAccounts list of the platform account owner.
     */
    public CompletionStage<Boolean> sendRegistrationConfirmation(@Nonnull Registration registration,
                                                                 @Nonnull List<PlatformAccount> platformAccounts) {
        if (!platformAccounts.isEmpty()) {
            // Create a new task and send a message per platform account.
            Task messageTask = new Task("registration_success");

            Message message = MessageHelper.buildConfirmDeviceMessage(registration, platformAccounts);
            message.addRegistrationToken(registration.registrationToken);
            messageTask.addMessage(message);

            // Add the message task to the TaskQueue.
            if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                mTaskQueue.addTask(messageTask, new SendAlertRecipientCallback());
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Result Callbacks from the dispatcher.
     */
    private class SendAlertRecipientCallback implements PushMessageCallback {

        @Override
        public void removeRecipients(List<Recipient> recipients) {
            mLog.d(TAG, String.format("There are %d recipients to delete locally.", recipients.size()));
            for (Recipient recipientToRemove : recipients) {
                mAccountService.deleteRegistration(recipientToRemove.token);
            }
        }

        @Override
        public void updateRecipients(List<UpdatedRecipient> recipients) {
            mLog.d(TAG, String.format("There are %d recipients to delete locally.", recipients.size()));
            for (UpdatedRecipient recipientToUpdate : recipients) {
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
