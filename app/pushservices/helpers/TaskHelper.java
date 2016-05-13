package pushservices.helpers;

import pushservices.enums.RecipientState;
import pushservices.exceptions.TaskValidationException;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;

import javax.annotation.Nonnull;
import java.util.Calendar;

/**
 * A set of general-purpose functions for {@link Task}s and Task children.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/6/16 Splendid Bits.
 */
@SuppressWarnings({"WeakerAccess", "ConstantConditions"})
public class TaskHelper {

    /**
     * Copies a task and throws an exception on fatal errors with the task.
     *
     * @param task The task to validate.
     * @throws TaskValidationException fired when there are irrecoverable problems with the task.
     */
    public static Task copyTask(@Nonnull Task task) throws TaskValidationException {
        verifyTask(task);

        try {
            return (Task) task.clone();

        } catch (CloneNotSupportedException e) {
            throw new TaskValidationException(e.getMessage());
        }
    }

    /**
     * Verifies a task and throws an exception on fatal errors with the task.
     *
     * @param task The task to validate.
     * @throws TaskValidationException fired when there are irrecoverable problems with the task.
     */
    public static void verifyTask(@Nonnull Task task) throws TaskValidationException {
        // Throw an exception on missing messages.
        if (!TaskHelper.isTaskProcessReady(task)) {
            throw new TaskValidationException("Task must include messages with at least one STATE_IDLE recipient.");
        }

        for (Message message : task.messages) {
            if (message.credentials == null) {
                throw new TaskValidationException("A Task Message is missing a Credentials model.");
            }

            if (message.credentials.platformType == null) {
                throw new TaskValidationException("A Message's Credentials is missing a PlatformType.");
            }

            boolean containsAuthorisationKey = message.credentials.authorisationKey != null
                    && !message.credentials.authorisationKey.isEmpty();

            boolean containsCertificateBody = message.credentials.certificateBody != null
                    && !message.credentials.certificateBody.isEmpty();

            if (!containsAuthorisationKey && !containsCertificateBody) {
                throw new TaskValidationException("A Message's Credentials has no AuthorisationKey or CertificateBody.");
            }
        }
    }

    /**
     * Check if a recipient is ready to send a message.
     *
     * @param recipient recipient to check.
     * @return true if the recipient is ready to be included in a platform message.
     */
    public static boolean isRecipientReady(@Nonnull Recipient recipient) {
        Calendar currentTime = Calendar.getInstance();

        // Add the registration to the new message.
        return ((recipient.state.equals(RecipientState.STATE_WAITING_RETRY) ||
                recipient.state.equals(RecipientState.STATE_IDLE) ||
                recipient.state.equals(RecipientState.STATE_PROCESSING)) &&
                recipient.nextAttempt == null || recipient.nextAttempt.after(currentTime));
    }

    /**
     * Returns true if the {@link Task} ready to be dispatched (not waiting for next interval
     * and hasn't failed or isn't currently processing)
     *
     * @param task The task to start work on.
     * @return true if the task can be processed. false if it is invalid or has completed..
     */
    public static boolean isTaskProcessReady(@Nonnull Task task) {
        if (task.messages != null && !task.messages.isEmpty()) {
            for (Message message : task.messages) {

                // If the message has at least 1 recipient,
                if (message.recipients != null && !message.recipients.isEmpty()) {
                    for (Recipient recipient : message.recipients) {

                        // If the state is empty, reset it to idle.
                        if (recipient.state == null) {
                            recipient.state = RecipientState.STATE_IDLE;
                            return true;
                        }

                        if (!recipient.state.equals(RecipientState.STATE_COMPLETE) &&
                                !recipient.state.equals(RecipientState.STATE_FAILED)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
