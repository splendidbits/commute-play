package services;

import helpers.MessageHelper;
import main.Log;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Task;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
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
     * Notify GCM subscribers of the modified alerts which have changed.
     *
     * @param modifiedAlerts Collection of modified alerts including their routes.
     */
    public CompletionStage<Boolean> notifySubscribersAsync(@Nonnull ModifiedAlerts modifiedAlerts) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                List<Alert> updatedAlerts = modifiedAlerts.getUpdatedAlerts();

                // Iterate through the routes.
                for (Alert alert : updatedAlerts) {

                    // Get all accounts with registrations subscribed to that route.
                    List<Account> accounts = mAccountService.getRegistrationAccounts(PLATFORM_NAME_GCM,
                            modifiedAlerts.getAgencyId(), alert.route);

                    // Loop through each sending API account.
                    if (accounts != null && !accounts.isEmpty()) {

                        // Create a new task and send 1 message per API account.
                        Task messageTask = new Task();
                        for (Account account : accounts) {

                            List<Registration> registrations = new ArrayList<>();
                            List<PlatformAccount> platformAccounts = new ArrayList<>();

                            for (PlatformAccount platformAccount : account.platformAccounts) {
                                if (account.registrations != null && !account.registrations.isEmpty()) {
                                    registrations.addAll(account.registrations);
                                }
                                if (account.platformAccounts != null) {
                                    platformAccounts.addAll(account.platformAccounts);
                                }
                            }

                            Message message = buildAlertMessage(alert, registrations, platformAccounts);
                            if (message != null) {
                                messageTask.addMessage(message);
                            }
                        }

                        if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                            // Add the message task to the TaskQueue.
                            mTaskQueue.addTask(messageTask);
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
     * @param registration registration for newly registered device.
     * @param platformAccounts list of the platform account owner.
     */
    public CompletionStage<Boolean> sendRegistrationConfirmation(@Nonnull Registration registration,
                                             @Nonnull List<PlatformAccount> platformAccounts) {
        if (!platformAccounts.isEmpty()) {
            // Create a new task and send a message per platform account.
            Task messageTask = new Task();

            Message message = MessageHelper.buildConfirmDeviceMessage(registration, platformAccounts);
            message.addRegistrationToken(registration.registrationToken);
            messageTask.addMessage(message);

            // Add the message task to the TaskQueue.
            if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                mTaskQueue.addTask(messageTask);
            }
        }
        return CompletableFuture.completedFuture(true);
    }

}
