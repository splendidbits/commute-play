package pushservices.helpers;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/8/16 Splendid Bits.
 */

import org.jetbrains.annotations.NotNull;
import pushservices.interfaces.RawMessageResponse;
import pushservices.models.app.MessageResult;
import pushservices.models.app.UpdatedRegistration;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.types.PushFailCause;
import pushservices.types.RecipientState;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * Sorts MessageResult information into the recipients of a message with recipient
 * states so that the message can be easily persisted.
 */
public abstract class SortedRecipientResponse implements RawMessageResponse {

    /**
     * This helper class, which implements {@link RawMessageResponse} sorts {@link MessageResult}
     * {@link Recipient}s back into the original sent {@link Message}. This MessageResponse to be
     * implement without doing basic heavy lifting of processing {@link Recipient}
     * {@link RecipientState}s.
     */
    @Override
    public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {

        /*
         *  The sorting strategies are as follows:
         *  1) Everything was okay, but some recipients need to be updated.
         *  2) There were stale recipients that failed and should be deleted.
         *  3) There were partial errors with some recipients and those need to be retried..
         */
        // Remove the original recipients in favour of using the MessageResult.
        message.recipients = new ArrayList<>();

        // 1) Successful Recipients
        for (Recipient recipient : result.getSuccessfulRecipients()) {
            recipient.state = RecipientState.STATE_COMPLETE;
            message.recipients.add(recipient);
        }

        // 2) Recipients to update.
        if (!result.getUpdatedRegistrations().isEmpty()) {
            for (UpdatedRegistration updatedRecipient : result.getUpdatedRegistrations()) {

                // Set the recipient as updated and add to message list.
                updatedRecipient.getUpdatedRegistration().state = RecipientState.STATE_COMPLETE;
                message.recipients.add(updatedRecipient.getUpdatedRegistration());
            }
        }

        // 3) Permanency failed recipients.
        if (!result.getStaleRecipients().isEmpty()) {
            for (Recipient recipient : result.getStaleRecipients()) {

                // Set the recipient as failed and add to message list.
                recipient.state = RecipientState.STATE_FAILED;
                message.recipients.add(recipient);
            }
        }

        // 4) Recipients to retry.
        if (!result.getRecipientsToRetry().isEmpty()) {
            for (Recipient recipientToRetry : result.getRecipientsToRetry()) {
                recipientToRetry.state = RecipientState.STATE_WAITING_RETRY;
                message.recipients.add(recipientToRetry);
            }
        }
    }

    @Override
    public void messageFailure(@Nonnull Message message, PushFailCause failCause) {
        for (Recipient recipient : message.recipients) {
            recipient.state = RecipientState.STATE_FAILED;
        }
    }
}
