package pushservices.interfaces;

import pushservices.models.app.MessageResult;
import pushservices.types.PushFailCause;
import pushservices.models.database.Message;

import javax.annotation.Nonnull;

/**
 * Response back from the push message pushservices (APNS or GCM) for the sent message.
 * The message may have either succeeded or failed.
 *
 * If the message failed, it was a "hard, unrecoverable" error, meaning that the message,
 * and all of it's recipients have been flagged as "FAILED'. If the message is the only message within
 * the task, then the task itself will also be flagged as failed.
 *
 * If the message was processed by the pushservices, then the message has somewhat succeeded. As you notice,
 * the interface method is not called "messageSuccess", as the Recipients returned may be a mixture of
 * success, failures, and retries.
 */
public interface RawMessageResponse {

    /**
     * A callback for results returned for a message send from the pushservices.
     *
     * @param sentMessage The original message send to the pushservices
     * @param result      The result back from the pushservices. Includes recipient send data.
     */
    void messageResult(@Nonnull Message sentMessage, @Nonnull MessageResult result);

    void messageFailure(@Nonnull Message message, PushFailCause failCause);
}
