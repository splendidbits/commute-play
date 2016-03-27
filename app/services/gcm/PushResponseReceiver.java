package services.gcm;

import interfaces.IPushResponse;
import models.app.MessageResult;

public class PushResponseReceiver implements IPushResponse {

    @Override
    public void messageSuccess(MessageResult result) {

    }

    @Override
    public void messageFailed(MessageResult result) {
    }
}
