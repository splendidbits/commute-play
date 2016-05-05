package pushservices.services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.annotation.Transactional;
import main.Log;
import org.jetbrains.annotations.NotNull;
import pushservices.enums.PlatformFailureType;
import pushservices.helpers.SortedRecipientResponse;
import pushservices.interfaces.TaskMessageResponse;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.RecipientFailure;
import pushservices.models.database.Task;
import pushservices.types.PushFailCause;
import pushservices.types.RecipientState;
import pushservices.types.TaskState;

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
    private ConcurrentHashMap<Long, TaskMessageResponse> mTaskIdClientListeners = new ConcurrentHashMap<>();
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
        try {
            List<Task> tasks = mEbeanServer.find(Task.class)
                    .order("id")
                    .fetch("messages", new FetchConfig().query())
                    .fetch("messages.recipients", new FetchConfig().query())
                    .fetch("messages.credentials", new FetchConfig().query())
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
        } catch (Exception e) {
            mLog.e(TAG, "Error getting message tasks.", e);
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
    public boolean addTask(@Nonnull Task task, TaskMessageResponse taskMessageResponse) {
        mLog.d(TAG, "Saving new task to TaskQueue and datastore.");
        for (Task queuedTask : mPendingTaskQueue) {
            if (Objects.equals(queuedTask.id, task.id)) {
                return false;
            }
        }

        // Add to the queue in a new thread.
        if (task.state == null || (!task.state.equals(TaskState.STATE_COMPLETE) &&
                !task.state.equals(TaskState.STATE_FAILED))) {

            if (task.messages != null) {
                for (Message message : task.messages) {
                    if (message.credentials != null) {
                        mEbeanServer.refresh(message.credentials);
                    }
                }
            }

            // Insert the task entry and dd the client callback.
            saveTask(task);
            if (taskMessageResponse != null) {
                mTaskIdClientListeners.put(task.id, taskMessageResponse);
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
        Transaction transaction = mEbeanServer.createTransaction();

        try {
            if (task.id != null) {
                Task foundTask = mEbeanServer
                        .find(Task.class)
                        .fetch("messages")
                        .fetch("messages.recipients")
                        .fetch("messages.credentials")
                        .where()
                        .eq("id", task.id)
                        .findUnique();

                if (foundTask != null) {
                    foundValidTask = true;

                    task.id = foundTask.id;
                    mEbeanServer.update(task, transaction);
                    transaction.commit();
                }
            }

            // If the task was not found, insert a new task.
            if (!foundValidTask) {
                mEbeanServer.save(task, transaction);
                transaction.commit();
            }

        } catch (Exception exception) {
            transaction.rollback();
            mLog.e(TAG, "Error saving new task state", exception);
        }
    }

    /**
     * Response back from the push message pushservices (APNS or GCM) for the sent message.
     * The message may have either succeeded or failed.
     * <p>
     * As {@link SortedRecipientResponse} is being extended, instead of simply implementing
     * the {@link SortedRecipientResponse} callback, the {@link Recipient}s are
     * sorted back into the original sent message.
     */
    private class MessageResponseListener extends SortedRecipientResponse {
        private Task mOriginalTask = null;

        public MessageResponseListener(Task originalTask) {
            mOriginalTask = originalTask;
        }

        @Override
        public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {
            super.messageResult(message, result);

            if (message.recipients != null) {
                mLog.d(TAG, String.format("200-OK from pushservices for message %d", message.id));
                TaskMessageResponse clientTaskCallback = mTaskIdClientListeners.get(mOriginalTask.id);

                // Update the time.
                mOriginalTask.lastAttempt = Calendar.getInstance();

                /*
                 * If the message (task) needs retrying. Set the Task as TaskState.STATE_PARTIALLY_PROCESSED
                 * and Recipient states as RecipientState. STATE_PARTIALLY_PROCESSED (unless we have reached
                 * the maximum amount of allowed retries, in which case set the TaskState.STATE_FAILED and each
                 * Recipient as RecipientState.STATE_FAILED.
                 */
                if (!result.getRecipientsToRetry().isEmpty()) {
                    if (mOriginalTask.retryCount <= MAXIMUM_TASK_RETRIES) {

                        // Set the task as partially processed.
                        mOriginalTask.state = TaskState.STATE_PARTIALLY_PROCESSED;
                        mOriginalTask.retryCount = mOriginalTask.retryCount + 1;

                        // Exponentially bump up the next retry time.
                        Calendar nextAttemptCal = Calendar.getInstance();
                        nextAttemptCal.add(Calendar.MINUTE, (mOriginalTask.retryCount * 2));
                        mOriginalTask.nextAttempt = nextAttemptCal;

                    } else {
                        // If the max retry count was reached, complete the task, and fail remaining recipients.
                        mOriginalTask.state = TaskState.STATE_FAILED;

                        for (Recipient messageRecipient : message.recipients) {
                            if (messageRecipient.state == RecipientState.STATE_WAITING_RETRY) {
                                messageRecipient.state = RecipientState.STATE_FAILED;
                                messageRecipient.failure = new RecipientFailure(PlatformFailureType.ERROR_TOO_MANY_RETRIES);
                            }
                        }
                    }
                } else {
                    // If there were no retry recipients, mark the task as complete.
                    mOriginalTask.state = TaskState.STATE_COMPLETE;
                    for (Recipient messageRecipient : message.recipients) {
                        messageRecipient.state = RecipientState.STATE_COMPLETE;
                    }
                }

                // Send back any recipients to be updated.
                if (!result.getUpdatedRegistrations().isEmpty() && clientTaskCallback != null) {
                    clientTaskCallback.updateRecipients(result.getUpdatedRegistrations());
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
                    clientTaskCallback.completed(mOriginalTask);
                    removeClientListener(clientTaskCallback);
                }

                // Update the task.
                saveTask(mOriginalTask);
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, PushFailCause failCause) {
            TaskMessageResponse clientTaskCallback = mTaskIdClientListeners.get(mOriginalTask.id);

            // Set the task as failed.
            mOriginalTask.state = TaskState.STATE_FAILED;

            if (clientTaskCallback != null) {
                clientTaskCallback.failed(mOriginalTask, failCause);
                removeClientListener(clientTaskCallback);
            }

            // Update the task.
            saveTask(mOriginalTask);

            mLog.e(TAG, String.format("Failed. %s response from push service for message %d",
                    failCause.name(), message.id));
        }

        /**
         * Remove a client's {@link TaskMessageResponse} listener if it's not null and
         * the {@link Task} has completed or failed.
         */
        private void removeClientListener(@Nonnull TaskMessageResponse taskMessageResponse) {
            if (mOriginalTask.state == TaskState.STATE_COMPLETE || mOriginalTask.state == TaskState.STATE_FAILED) {
                mTaskIdClientListeners.remove(mOriginalTask.id);
            }
        }
    }

    /**
     * Check if a task is stale (has no messages or no recipients in any messages).
     *
     * @param task Task to check/
     * @return boolean true if the task is stale.
     */
    private boolean isTaskEmpty(@Nonnull Task task) {
        if (task.messages != null) {
            for (Message message : task.messages) {
                if (message.recipients != null && !message.recipients.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Thread that loops through the blocking queue and sends
     * tasks to the pushservices. This will block if there are no tasks to take,
     * hence the Runnable.
     */
    private class QueueConsumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    mLog.d(TAG, "TaskQueue Consumer waiting to take element..");
                    Task task = mPendingTaskQueue.take();

                    if (isTaskEmpty(task) || task.state.equals(TaskState.STATE_COMPLETE)) {
                        // Clean up complete or empty tasks.

                        mLog.d(TAG, "Removing stale task from TaskQueue.");
                        task.state = TaskState.STATE_COMPLETE;
                        saveTask(task);
                        mPendingTaskQueue.remove();

                    } else if (isTaskIncomplete(task) && task.messages != null) {
                        // Start updating the task models for dispatch.

                        task.state = TaskState.STATE_PROCESSING;
                        task.lastAttempt = Calendar.getInstance();

                        // Set all recipient states on the pending task to processing.
                        for (Message message : task.messages) {
                            if (message.recipients != null) {
                                for (Recipient recipient : message.recipients) {
                                    recipient.state = RecipientState.STATE_PROCESSING;
                                }
                            }
                        }

                        // Persist the and process the task.
                        saveTask(task);
                        mLog.d(TAG, "TaskQueue processing received task");

                        // Dispatch each message within the task..
                        for (Message message : task.messages) {
                            if (message.recipients != null && !message.recipients.isEmpty()) {
                                mGcmDispatcher.dispatchMessageAsync(message, new MessageResponseListener(task));
                            }
                        }
                    }

                    // Sleep every second to cool down.
                    Thread.sleep(TASK_POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                mLog.d(TAG, "TaskQueue consumer thread was interrupted.");
            }
        }
    }
}