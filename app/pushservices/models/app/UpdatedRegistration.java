package pushservices.models.app;

import pushservices.models.database.Recipient;

import javax.annotation.Nonnull;

/**
 * Defines a {@link Recipient} to be updated by the client. Includes
 * the original stale {@link Recipient} details and the new Recipient details.
 */
public class UpdatedRegistration {
    private Recipient mStaleRecipient;
    private Recipient mUpdatedRecipient;

    private UpdatedRegistration() {
    }

    public UpdatedRegistration(@Nonnull Recipient staleRegistration, @Nonnull Recipient updatedRegistration) {
        mStaleRecipient = staleRegistration;
        mUpdatedRecipient = updatedRegistration;
    }

    public Recipient getUpdatedRegistration() {
        return mUpdatedRecipient;
    }

    public Recipient getStaleRegistration() {
        return mStaleRecipient;
    }
}