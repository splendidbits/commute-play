package pushservices.helpers;

import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;
import pushservices.types.RecipientState;
import pushservices.types.TaskState;

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
        clonedMessage.ttlSeconds = message.ttlSeconds;
        clonedMessage.isDryRun = message.isDryRun;
        clonedMessage.shouldDelayWhileIdle = message.shouldDelayWhileIdle;
        clonedMessage.messagePriority = message.messagePriority;
        clonedMessage.task = message.task;
        clonedMessage.sentTime = message.sentTime;

        return clonedMessage;
    }

    /**
     * Returns true if the {@link Task} ready to be dispatched (not waiting for next interval
     * and hasn't failed or isn't currently processing)
     *
     * @param task The task to start work on.
     * @return true if the task can be processed. false if it is invalid or has completed..
     */
    public static boolean isTaskIncomplete(@Nonnull Task task) {
        boolean taskReady;
        if (task.state == null) {
            task.state = TaskState.STATE_IDLE;
        }

        taskReady = !task.state.equals(TaskState.STATE_COMPLETE) &&
                !task.state.equals(TaskState.STATE_FAILED);

        // Check task has messages
        if (task.messages == null || task.messages.isEmpty()) {
            return false;
        }

        // Check task has at least some recipients
        boolean messagesReady = false;
        for (Message message : task.messages) {

            // If the message has at least 1 recipient,
            if (message.recipients != null && !message.recipients.isEmpty()) {
                for (Recipient recipient : message.recipients) {

                    // If the state is empty, reset it to idle.
                    if (recipient.state == null) {
                        recipient.state = RecipientState.STATE_IDLE;
                        messagesReady = true;
                    }

                    if (!recipient.state.equals(RecipientState.STATE_COMPLETE) &&
                            !recipient.state.equals(RecipientState.STATE_FAILED)) {
                        messagesReady = true;
                    }
                }
            }
        }

        return messagesReady && taskReady;
    }
}
