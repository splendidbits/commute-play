package pushservices.interfaces;

import pushservices.models.app.UpdatedRegistration;
import pushservices.types.PushFailCause;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Callbacks for actions that should be taken by the client on response from the
 * Google GCM server. These things include deleting a stale recipient such as (when they are
 * not registered), or when the Recipient information has changed and the client should
 * update it.
 */
public interface TaskMessageResponse {
    void removeRecipients(@Nonnull List<Recipient> recipients);

    void updateRecipients(@Nonnull List<UpdatedRegistration> registrations);

    void completed(@Nonnull Task task);

    void failed(@Nonnull Task task, @Nonnull PushFailCause reason);
}