package dispatcher.interfaces;

import dispatcher.models.MessageResult;
import dispatcher.types.PushFailCause;
import models.taskqueue.Message;

import javax.annotation.Nonnull;

/**
 * Response back from the push message dispatcher (APNS or GCM) for the sent message.
 * The message may have either succeeded or failed.
 *
 * If the message failed, it was a "hard, unrecoverable" error, meaning that the message,
 * and all of it's recipients have been flagged as "FAILED'. If the message is the only message within
 * the task, then the task itself will also be flagged as failed.
 *
 * If the message was processed by the dispatcher, then the message has somewhat succeeded. As you notice,
 * the interface method is not called "messageSuccess", as the Recipients returned may be a mixture of
 * success, failures, and retries.
 */
public interface MessageResponse {

    /**
     * A callback for results returned for a message send from the dispatcher.
     *
     * @param sentMessage The original message send to the dispatcher
     * @param result      The result back from the dispatcher. Includes recipient send data.
     */
    void messageResult(@Nonnull Message sentMessage, @Nonnull MessageResult result);

    void messageFailure(@Nonnull Message message, PushFailCause failCause);
}
