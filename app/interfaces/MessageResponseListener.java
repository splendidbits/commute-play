package interfaces;

import models.app.MessageResult;
import models.taskqueue.Message;

import javax.annotation.Nonnull;

/**
 * Interface callback to allow clients to know the state of a particular
 * outbound Google GCM or APNS push message.
 */
public interface MessageResponseListener {
    enum HardFailCause {
        ENDPOINT_NOT_FOUND,
        ENDPOINT_TIMEOUT,
        AUTH_ERROR,
        GCM_MESSAGE_JSON_ERROR,
        MISSING_RECIPIENTS,
        UNKNOWN_ERROR
    }

    void messageResult(@Nonnull MessageResult result);
    void messageFailed(@Nonnull Message message, HardFailCause failCause);
}