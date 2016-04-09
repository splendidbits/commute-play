package dispatcher.interfaces;

import dispatcher.models.UpdatedRecipient;
import dispatcher.types.PushFailCause;
import models.taskqueue.Recipient;
import models.taskqueue.Task;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Callbacks for actions that should be taken by the client on response from the
 * Google GCM server. These things include deleting a stale recipient such as (when they are
 * not registered), or when the Recipient information has changed and the client should
 * update it.
 */
public interface PushMessageCallback {
    void removeRecipients(@Nonnull List<Recipient> recipients);

    void updateRecipients(@Nonnull List<UpdatedRecipient> recipients);

    void completed(@Nonnull Task task);

    void failed(@Nonnull Task task, @Nonnull PushFailCause reason);
}