package pushservices.helpers;

import pushservices.models.database.Message;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/6/16 Splendid Bits.
 */
public class MessageHelper {

    /**
     * Copy a message and all child models completely, excluding any registration information
     *
     * @param message message to copy.
     * @return a cloned message with removed recipient information.
     */
    @Nonnull
    public static Message copyMessage(@Nonnull Message message) {
        Message clonedMessage = new Message();
        clonedMessage.id = message.id;
        clonedMessage.credentials = message.credentials;
        clonedMessage.recipients = new ArrayList<>();
        clonedMessage.payloadData = message.payloadData;
        clonedMessage.collapseKey = message.collapseKey;
        clonedMessage.ttl = message.ttl;
        clonedMessage.isDryRun = message.isDryRun;
        clonedMessage.shouldDelayWhileIdle = message.shouldDelayWhileIdle;
        clonedMessage.messagePriority = message.messagePriority;
        clonedMessage.task = message.task;
        clonedMessage.sentTime = message.sentTime;

        return clonedMessage;
    }
}
