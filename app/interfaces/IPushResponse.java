package interfaces;

import models.app.MessageResult;

import javax.annotation.Nonnull;

/**
 * Interface callback to allow clients to know the state of a particular
 * outbound Google GCM or APNS push message.
 */
public interface IPushResponse {
    void messageSuccess(@Nonnull MessageResult result);
    void messageFailed(@Nonnull MessageResult result);
}