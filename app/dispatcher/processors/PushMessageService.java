package dispatcher.processors;

import dispatcher.interfaces.PushMessageCallback;
import dispatcher.models.UpdatedRecipient;
import dispatcher.types.PushFailCause;
import helpers.MessageHelper;
import main.Log;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.Task;
import services.AccountService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static helpers.MessageHelper.buildAlertMessage;
import static models.accounts.Platform.PLATFORM_NAME_GCM;

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
     * @param alerts Collection of modified alerts including their routes.
     */
    public CompletionStage<Boolean> notifyAlertSubscribers(@Nonnull ModifiedAlerts alerts) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                List<Alert> updatedAlerts = alerts.getUpdatedAlerts();

                // Iterate through the Alerts to send.
                mLog.d(TAG, String.format("Found %d alerts to be sent to subscribers", updatedAlerts.size()));
                for (Alert alert : updatedAlerts) {

                    // Get all accounts with registrations subscribed to that route.
                    List<Account> accounts = mAccountService.getRegistrationAccounts(
                            PLATFORM_NAME_GCM, alerts.getAgencyId(), alert.route);

                    // Iterate through each sending API account.
                    mLog.d(TAG, String.format("Found %d accounts for alert %s",
                            updatedAlerts.size(), alert.route.routeId));

                    if (accounts != null && !accounts.isEmpty()) {

                        // Create a new task and build 1 message per API account.
                        Task messageTask = new Task(alert.route.routeName);
                        for (Account account : accounts) {

                            Message message = buildAlertMessage(alert,
                                    account.registrations,
                                    account.platformAccounts);

                            if (message != null) {
                                messageTask.addMessage(message);
                            }
                        }

                        // Finally, if there are messages in the task, add it to the TaskQueue.
                        if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                            mTaskQueue.addTask(messageTask, new SendAlertRecipientCallback());
                            completableFuture.complete(true);

                        } else {
                            completableFuture.complete(false);
                        }
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
    private class SendAlertRecipientCallback implements PushMessageCallback{

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
