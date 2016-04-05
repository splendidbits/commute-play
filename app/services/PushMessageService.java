package services;

import helpers.GcmMessageHelper;
import main.Log;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Task;
import play.api.Play;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static helpers.GcmMessageHelper.buildAlertMessage;
import static models.accounts.Platform.PLATFORM_NAME_GCM;

/**
 * Application-wide service for sending information through the
 * platform push channels.
 */
public class PushMessageService {
    private static final String TAG = PushMessageService.class.getSimpleName();

    private AccountService mAccountService;
    private Log mLog = Play.current().injector().instanceOf(Log.class);
    private TaskQueue mTaskQueue = Play.current().injector().instanceOf(TaskQueue.class);

    public PushMessageService(AccountService accountService) {
        mAccountService = accountService;
    }

    /**
     * Notify GCM subscribers of the modified alerts which have changed.
     *
     * @param modifiedAlerts Collection of modified alerts including their routes.
     */
    public void notifyAlertSubscribers(@Nonnull ModifiedAlerts modifiedAlerts) {
        List<Alert> updatedAlerts = modifiedAlerts.getUpdatedAlerts();

        // Iterate through the routes.
        for (Alert alert : updatedAlerts) {

            // Get all accounts for the registrations subscribed to that route.
            List<Account> accounts = mAccountService.getRegistrationAccounts(
                    PLATFORM_NAME_GCM,
                    modifiedAlerts.getAgencyId(),
                    alert.route);

            // Loop through each sending API account.
            if (accounts != null && !accounts.isEmpty()) {

                // Create a new task and send a logMessage per platform account.
                Task messageTask = new Task();
                for (Account account : accounts) {

                    for (PlatformAccount platformAccount : account.platformAccounts) {
                        if (account.registrations != null && !account.registrations.isEmpty()) {
                            Message message = buildAlertMessage(alert, account.registrations, platformAccount);
                            messageTask.addMessage(message);

                        } else {
                            mLog.i(TAG, "Outbound message not build as there were 0 recipients");
                        }
                    }
                }

                // Add the message task to the TaskQueue.
                if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                    mTaskQueue.addTask(messageTask);
                }
            }
        }
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
            for (PlatformAccount platformAccount : platformAccounts) {

                // Loop through each sending API account.
                if (platformAccount.platform.platformName.equals(PLATFORM_NAME_GCM)) {

                    Message message = GcmMessageHelper.buildConfirmDeviceMessage(registration, platformAccount);
                    message.addRegistrationId(registration.registrationToken);
                    messageTask.addMessage(message);
                }
            }

            // Add the message task to the TaskQueue.
            if (messageTask.messages != null && !messageTask.messages.isEmpty()) {
                mTaskQueue.addTask(messageTask);
            }
        }
        return CompletableFuture.completedFuture(true);
    }

}
