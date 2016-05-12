package pushservices.interfaces;

import pushservices.enums.FailureType;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;

import javax.annotation.Nonnull;

/**
 * Response back from the push message push-services (APNS or GCM) for the sent message.
 * The message may have either succeeded or failed.
 *
 * If the message failed, it was a "hard, unrecoverable" error, meaning that the message,
 * and all of it's recipients have been flagged as "FAILED'. If the message is the only message within
 * the task, then the task itself will also be flagged as failed.
 *
 * If the message was processed by the push-services, then the message has somewhat succeeded. As you notice,
 * the interface method is not called "messageSuccess", as the Recipients returned may be a mixture of
 * success, failures, and retries.
 */
public interface PlatformMessageResponse {

    /**
     * A callback for results returned for a message send from the push-services.
     *
     * @param message The original message send to the push-services
     * @param result      The result back from the push-services. Includes recipient send data.
     */
    void messageResult(@Nonnull Message message, @Nonnull MessageResult result);

    void messageFailure(@Nonnull Message message, FailureType failureType);
}
