package pushservices.interfaces;

import org.jetbrains.annotations.NotNull;
import pushservices.enums.FailureType;
import pushservices.enums.RecipientState;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Sorts MessageResult information into the recipients of a message with recipient
 * states so that the message can be easily persisted.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/8/16 Splendid Bits.
 */
public abstract class RecipientPlatformMessageResponse implements PlatformMessageResponse {

    /**
     * This helper class, which implements {@link PlatformMessageResponse} updates a {@link MessageResult}
     * list of {@link Recipient}. The Message Recipient sorting strategy is as follows:
     * <p>
     * - Some recipients succeeded, but some recipients need to be updated.
     * - There were soft errors with some recipients and those need to be retried.
     * <p>
     * Message Recipients are sorted into the correct result collection and the
     * {@link RecipientState} and exponential backoff attributes are set accordingly.
     */
    @Override
    public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {

        // If there were recipients to retry, modify the exponential backoff time, etc.
        if (!result.getRecipientsToRetry().isEmpty()) {
            Calendar nextAttemptCal = Calendar.getInstance();

            Iterator<Recipient> recipientIterator = result.getRecipientsToRetry().iterator();
            while (recipientIterator.hasNext()) {
                Recipient recipient = recipientIterator.next();

                // Update the new backoff time, and count.
                if (recipient.sendAttemptCount <= message.maximumRetries) {
                    recipient.state = RecipientState.STATE_WAITING_RETRY;

                    recipient.sendAttemptCount = recipient.sendAttemptCount + 1;
                    nextAttemptCal.add(Calendar.MINUTE, (recipient.sendAttemptCount * 2));
                    recipient.nextAttempt = nextAttemptCal;

                } else {
                    // Move failed recipient to the failed result collection.
                    recipient.state = RecipientState.STATE_FAILED;
                    result.addFailedRecipients(recipient, recipient.failure);
                    recipientIterator.remove();
                }
            }
        }

        // Mark successful recipients as complete.
        if (!result.getSuccessfulRecipients().isEmpty()) {
            for (Recipient recipient : result.getSuccessfulRecipients()) {
                recipient.state = RecipientState.STATE_COMPLETE;
            }
        }

        // Mark failed recipients as failed.
        if (!result.getFailedRecipients().isEmpty()) {
            for (Recipient recipient : result.getFailedRecipients().keySet()) {
                recipient.state = RecipientState.STATE_FAILED;
            }
        }
    }

    /**
     * A message failed to send with fatal errors, and no retries are possible for
     * any of the message recipients. All Recipients are marked as STATE_FAILED.
     *
     * @param message Message that failed.
     * @param failureType The type of failure.
     */
    @Override
    public void messageFailure(@Nonnull Message message, FailureType failureType) {
        if (message.recipients != null) {
            for (Recipient recipient : message.recipients) {
                recipient.state = RecipientState.STATE_FAILED;
            }
        }
    }
}
