package dispatcher.processors;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.annotation.Transactional;
import dispatcher.interfaces.PushMessageCallback;
import dispatcher.models.MessageResult;
import dispatcher.models.UpdatedRecipient;
import dispatcher.types.PushFailCause;
import dispatcher.types.RecipientState;
import dispatcher.types.TaskState;
import main.Log;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.RecipientFailure;
import models.taskqueue.Task;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 * The singleton class that handles all GCM task jobs.
 * <p>
 * TODO: Move database related functions to a TaskDAO.
 */
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private static final int MAXIMUM_TASK_RETRIES = 10;
    private static final long TASK_POLL_INTERVAL_MS = 1000;

    private TransferQueue<Task> mPendingTaskQueue = new LinkedTransferQueue<>();
    private ConcurrentHashMap<Long, PushMessageCallback> mTaskIdClientListeners = new ConcurrentHashMap<>();
    private QueueConsumer mQueueConsumer = new QueueConsumer();
    private Thread mTaskConsumerThread;

    private GcmDispatcher mGcmDispatcher;
    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public TaskQueue(EbeanServer ebeanServer, Log log, GcmDispatcher gcmDispatcher) {
        mEbeanServer = ebeanServer;
        mLog = log;
        mGcmDispatcher = gcmDispatcher;
        mTaskConsumerThread = new Thread(mQueueConsumer);

        // Fetch any tasks that are partially or incomplete.
        fetchPendingTasks();
        start();
    }

    /**
     * Returns if the {@link Task} ready to be dispatched (not waiting for next interval
     * and hasn't failed or isn't currently processing)
     *
     * @param newTask The task to start work on.
     * @return true or false, duh.
     */
    private boolean isTaskIncomplete(@Nonnull Task newTask) {
        Calendar currentTime = Calendar.getInstance();
        return (!newTask.state.equals(TaskState.STATE_COMPLETE) &&
                !newTask.state.equals(TaskState.STATE_FAILED) &&
                (newTask.nextAttempt == null || !newTask.nextAttempt.after(currentTime)));
    }

    /**
     * Get all {@link Task}s from the database which have not yet completed fully.
     */
    private void fetchPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .fetch("messages", new FetchConfig().query())
                .fetch("messages.recipients", new FetchConfig().query())
                .where()
                .conjunction()
                .ne("state", TaskState.STATE_FAILED)
                .ne("state", TaskState.STATE_COMPLETE)
                .endJunction()
                .filterMany("messages.recipients").ne("state", RecipientState.STATE_COMPLETE)
                .filterMany("messages.recipients").ne("state", RecipientState.STATE_FAILED)
                .findList();

        // Add the saved pending tasks back into the queue
        if (tasks != null) {
            for (Task task : tasks) {
                mPendingTaskQueue.offer(task);
            }
        }
    }

    /**
     * Starts the TaskQueue {@link Task} polling process.
     */
    public void start() {
        if (!mTaskConsumerThread.isAlive()) {
            mLog.d(TAG, "Starting the TaskQueue consumer thread.");
            mTaskConsumerThread.start();
        }
    }

    /**
     * Add a new task to the TaskQueue (don't add tasks that are already saved).
     *
     * @param task task to save and add to TaskQueue.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean addTask(@Nonnull Task task, PushMessageCallback pushMessageCallback) {
        mLog.d(TAG, "Saving new task to TaskQueue and datastore.");
        for (Task currentTask : mPendingTaskQueue) {
            if (Objects.equals(currentTask.id, task.id)) {
                return false;
            }
        }

        // Add to the queue in a new thread.
        if (task.state == null || !task.state.equals(TaskState.STATE_COMPLETE) &&
                !task.state.equals(TaskState.STATE_FAILED)) {

            // Insert the task entry and dd the client callback.
            saveTask(task);
            if (pushMessageCallback != null) {
                mTaskIdClientListeners.put(task.id, pushMessageCallback);
            }

            // Offer the task to the polling thread.
            CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    mPendingTaskQueue.offer(task);
                }
            });

            return true;
        }
        return false;
    }

    /**
     * Updates or Inserts a task in the TaskQueue datastore.
     *
     * @param task the task to persist.
     */
    @Transactional
    private void saveTask(@Nonnull Task task) {
        boolean foundValidTask = false;

        try {
            if (task.id != null) {
                Task foundTask = mEbeanServer
                        .find(Task.class)
                        .fetch("messages")
                        .fetch("messages.recipients")
                        .where()
                        .idEq(task.id)
                        .findUnique();

                if (foundTask != null) {
                    foundValidTask = true;

                    foundTask.name = task.name;
                    foundTask.messages = task.messages;
                    foundTask.state = task.state;
                    foundTask.retryCount = task.retryCount;
                    foundTask.lastAttempt = task.lastAttempt;
                    foundTask.nextAttempt = task.nextAttempt;
                    foundTask.taskAdded = task.taskAdded;

                    mEbeanServer.update(foundTask);
                }
            }

            // If the task was not found, insert a new task.
            if (!foundValidTask) {
                mEbeanServer.save(task);
            }

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task state", exception);
        }
    }

    /**
     * Response back from the push message dispatcher (APNS or GCM) for the sent message.
     * The message may have either succeeded or failed.
     * <p>
     * As {@link RecipientSortResponse} is being extended, instead of simply implementing
     * the {@link dispatcher.interfaces.MessageResponse} callback, the {@link Recipient}s are
     * sorted back into the original sent message.
     */
    private class MessageResponseListener extends RecipientSortResponse {
        private Task mTask = null;

        MessageResponseListener(Task task) {
            mTask = task;
        }

        @Override
        public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {
            super.messageResult(message, result);
            mLog.d(TAG, String.format("200-OK from dispatcher for message %d", message.id));

            PushMessageCallback clientTaskCallback = mTaskIdClientListeners.get(mTask.id);

            // Update the time.
            mTask.lastAttempt = Calendar.getInstance();

            /*
             * If the message (task) needs retrying. Set the Task as TaskState.STATE_PARTIALLY_PROCESSED
             * and Recipient states as RecipientState. STATE_PARTIALLY_PROCESSED (unless we have reached
             * the maximum amount of allowed retries, in which case set the TaskState.STATE_FAILED and each
             * Recipient as RecipientState.STATE_FAILED.
             */
            if (!result.getRecipientsToRetry().isEmpty()) {
                if (mTask.retryCount <= MAXIMUM_TASK_RETRIES) {

                    // Set the task as partially processed.
                    mTask.state = TaskState.STATE_PARTIALLY_PROCESSED;
                    mTask.retryCount = mTask.retryCount + 1;

                    // Exponentially bump up the next retry time.
                    Calendar nextAttemptCal = Calendar.getInstance();
                    nextAttemptCal.add(Calendar.MINUTE, (mTask.retryCount * 2));
                    mTask.nextAttempt = nextAttemptCal;

                } else {
                    // If the maximum retry count has been hit, complete the task, and fail the
                    // remaining recipients.
                    mTask.state = TaskState.STATE_FAILED;

                    for (Recipient messageRecipient : message.recipients) {
                        if (messageRecipient.state == RecipientState.STATE_WAITING_RETRY) {
                            messageRecipient.state = RecipientState.STATE_FAILED;
                            messageRecipient.failure = new RecipientFailure("MaxRetryReached");
                        }
                    }
                }
            } else {
                // If there were no retry recipients, mark the task as complete.
                mTask.state = TaskState.STATE_COMPLETE;
                for (Recipient messageRecipient : message.recipients) {
                    messageRecipient.state = RecipientState.STATE_COMPLETE;
                }
            }

            // Send back any recipients to be updated.
            if (!result.getUpdatedRegistrations().isEmpty() && clientTaskCallback != null) {
                List<UpdatedRecipient> updateRecipients = new ArrayList<>();
                for (Map.Entry<Recipient, Recipient> entry : result.getUpdatedRegistrations().entrySet()) {
                    updateRecipients.add(new UpdatedRecipient(entry.getKey(), entry.getValue()));
                }
                clientTaskCallback.updateRecipients(updateRecipients);
            }

            // Send back any recipients to be deleted.
            if (!result.getStaleRecipients().isEmpty() && clientTaskCallback != null) {
                List<Recipient> staleRecipients = new ArrayList<>();
                for (Recipient staleRecipient : result.getStaleRecipients()) {
                    staleRecipients.add(staleRecipient);
                }
                clientTaskCallback.removeRecipients(staleRecipients);
            }

            if (clientTaskCallback != null) {
                clientTaskCallback.completed(mTask);
                removeClientListener(clientTaskCallback);
            }

            // Update the task.
            Task updatedTask = updateTaskMessage(mTask, message);
            saveTask(updatedTask);
        }

        @Override
        public void messageFailure(@Nonnull Message message, PushFailCause failCause) {
            super.messageFailure(message, failCause);
            mLog.e(TAG, String.format("Failed. %s response from dispatcher for message %d",
                    failCause.name(), message.id));

            PushMessageCallback clientTaskCallback = mTaskIdClientListeners.get(mTask.id);

            // Set the task as failed.
            mTask.state = TaskState.STATE_FAILED;

            if (clientTaskCallback != null) {
                clientTaskCallback.failed(mTask, failCause);
                removeClientListener(clientTaskCallback);
            }

            // Update the task.
            Task updatedTask = updateTaskMessage(mTask, message);
            saveTask(updatedTask);
        }

        /**
         * Remove a client's {@link PushMessageCallback} listener if it's not null and
         * the {@link Task} has completed or failed.
         */
        private void removeClientListener(PushMessageCallback pushMessageCallback) {
            if (pushMessageCallback != null) {
                if (mTask.state == TaskState.STATE_COMPLETE || mTask.state == TaskState.STATE_FAILED) {
                    mTaskIdClientListeners.remove(mTask.id);
                }
            }
        }

        /**
         * Safely update a persisted message in the datastore.
         *
         * @param task    the task to update.
         * @param message the new message
         * @return updated task.
         */
        private Task updateTaskMessage(Task task, Message message) {
            // Update the message in the task.
            List<Message> taskMessages = new ArrayList<>(task.messages);
            Iterator<Message> messageIterator = taskMessages.iterator();
            while (messageIterator.hasNext()) {
                Message taskMessage = messageIterator.next();

                if (taskMessage.id.equals(message.id)) {
                    messageIterator.remove();
                    taskMessages.add(message);
                    break;
                }
            }
            task.messages = taskMessages;
            return task;
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * tasks to the dispatcher. This will block if there are no tasks to take,
     * hence the Runnable.
     */
    private class QueueConsumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    mLog.d(TAG, "Consumer is waiting to take element...");
                    Task task = mPendingTaskQueue.take();

                    mLog.d(TAG, "Consumer received Element: " + task.id);
                    if (isTaskIncomplete(task)) {

                        // Set the timestamp for now.
                        task.lastAttempt = Calendar.getInstance();

                        // Set the task and recipients as processing
                        task.state = TaskState.STATE_PROCESSING;
                        for (Message message : task.messages) {
                            for (Recipient recipient : message.recipients) {
                                recipient.state = RecipientState.STATE_PROCESSING;
                            }
                        }

                        // Persist the new task.
                        saveTask(task);

                        // Dispatch each message within the task..
                        for (Message message : task.messages) {
                            mGcmDispatcher.dispatchMessageAsync(message, new MessageResponseListener(task));
                        }
                    }

                    Thread.sleep(TASK_POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                mLog.d(TAG, "TaskQueue rotation consumer thread was interrupted and shut down.");
            }
        }
    }
}
