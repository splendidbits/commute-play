package pushservices.interfaces;

import pushservices.enums.FailureType;
import pushservices.models.app.UpdatedRegistration;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Callbacks for actions that should be taken by the client on response from the
 * Google GCM server.
 *
 * Those include deleting a stale recipient such as (when they are not registered),
 * or when the {@link Recipient} information has changed and the client should update it.
 */
public interface PlatformResponseCallback {

    /**
     * * Invoked with a list of recipients which are not valid for the Platform.
     *
     * @param recipients A list of invalid Platform {@link Recipient}s.
     */
    void removeRecipients(@Nonnull Collection<Recipient> recipients);

    /**
     * Invoked with a collection of updated {@link Recipient} registrations.
     *
     * @param updatedRegistrations A an old and new {@link Recipient} in a list of
     * {@link UpdatedRegistration}s.
     */
    void updateRecipients(@Nonnull Collection<UpdatedRegistration> updatedRegistrations);

    /**
     * The task was successfully processed and sent.
     *
     * @param task The completed Task.
     */
    void completed(@Nonnull Task task);

    /**
     * All messages in the task failed to dispatch with a {@link FailureType}.
     *
     * @param task The failed Task.
     * @param task The Platform failure.
     */
    void failed(@Nonnull Task task, @Nonnull FailureType failureType);
}