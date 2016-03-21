package interfaces;

import models.app.MessageResult;

/**
 * Interface callback to allow clients to know the state of a particular
 * outbound Google GCM or APNS message.
 */
public interface IPushResponse {
    void messageSuccess(MessageResult result);
    void messageFailed(MessageResult result);
}