package dispatcher.models;

import models.taskqueue.Recipient;

/**
 * Defines a {@link Recipient} to be updated by the client. Includes
 * the original stale {@link Recipient} details and the new Recipient details.
 */
public class UpdatedRecipient {
    private Recipient mStateRecipient;
    private Recipient mUpdatedRecipient;

    public UpdatedRecipient(Recipient updatedRecipient, Recipient stateRecipient) {
        mUpdatedRecipient = updatedRecipient;
        mStateRecipient = stateRecipient;
    }

    public Recipient getUpdatedRecipient() {
        return mUpdatedRecipient;
    }

    public Recipient getStateRecipient() {
        return mStateRecipient;
    }
}