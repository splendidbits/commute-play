package services;

import helpers.GcmMessageHelper;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Alert;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Task;
import services.gcm.GoogleGcmDispatcher;
import services.gcm.PushResponseReceiver;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static helpers.GcmMessageHelper.buildAlertMessage;
import static models.accounts.Platform.PLATFORM_NAME_GCM;

/**
 * Application-wide service for sending information through the
 * platform push channels.
 *
 * TODO: Migrate these methods to use the polling TaskQueue service.
 */
public class PushMessageService {

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
            AccountService accountService = new AccountService();
            List<Account> accounts = accountService.getRegistrationAccounts(
                    PLATFORM_NAME_GCM,
                    modifiedAlerts.getAgencyId(),
                    alert.route);

            // Loop through each sending API account.
            if (accounts != null && !accounts.isEmpty()) {

                // Create a new task and send a message per platform account.
                Task messageTask = new Task();
                for (Account account : accounts) {

                    for (PlatformAccount platformAccount : account.platformAccounts) {
                        Message message = buildAlertMessage(alert, account.registrations, platformAccount);
                        messageTask.addMessage(message);

                        // TODO: Remove this in favour of the polling queue.
                        new GoogleGcmDispatcher(message, new PushResponseReceiver());
                    }
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
    public void sendRegistrationConfirmation(@Nonnull Registration registration,
                                             @Nonnull List<PlatformAccount> platformAccounts) {

        // Create a new task and send a message per platform account.
        Task messageTask = new Task();

        for (PlatformAccount platformAccount : platformAccounts) {

            // Loop through each sending API account.
            if (platformAccount.platform.platformName.equals(PLATFORM_NAME_GCM)) {

                List<Message> taskMessages = new ArrayList<>();
                Message message = GcmMessageHelper.buildConfirmDeviceMessage(registration, platformAccount);
                messageTask.addMessage(message);

                // TODO: Remove this in favour of the polling queue.
                new GoogleGcmDispatcher(message, new PushResponseReceiver());
            }
        }
    }

}
