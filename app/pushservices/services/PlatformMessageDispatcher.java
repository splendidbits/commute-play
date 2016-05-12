package pushservices.services;

import pushservices.helpers.PlatformMessageBuilder;
import pushservices.interfaces.PlatformMessageResponse;
import pushservices.models.database.Message;

import javax.annotation.Nonnull;

/**
 * An abstract class for extending and creating a Message Dispatcher for a push
 * service platform.
 */
public abstract class PlatformMessageDispatcher {

    /**
     * Dispatch a message synchronously to the Platform endpoint, and get responses back through
     * a response interface.
     *
     * @param message          The constructed platform message to send. Build using
     *                         {@link PlatformMessageBuilder}.
     * @param responseListener The Platform response listener.
     */
    @SuppressWarnings("Convert2Lambda")
    public abstract void dispatchMessage(@Nonnull Message message, @Nonnull PlatformMessageResponse responseListener);

    /**
     * Thrown on a hard, breaking http call error such as 404.
     */
    protected class PlatformEndpointException extends RuntimeException {
        int mStatusCode = -1;
        String mErrorBody;

        PlatformEndpointException(int statusCode, String errorBody) {
            mErrorBody = errorBody;
            mStatusCode = statusCode;
        }
    }
}