package pushservices.services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
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
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The singleton class that handles all GCM task jobs.
 * <p>
 * TODO: Move database related functions to a TaskDAO.
 */
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private static final int MAXIMUM_TASK_RETRIES = 10;

    // Delay each taskqueue polling to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 500;
    private static final int TASKQUEUE_MAX_SIZE = 250;

    private ArrayBlockingQueue<Task> mPendingTaskQueue = new ArrayBlockingQueue<>(TASKQUEUE_MAX_SIZE);
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
    }

    /**
     * Returns true if the {@link Task} ready to be dispatched (not waiting for next interval
     * and hasn't failed or isn't currently processing)
     *
     * @param task The task to start work on.
     * @return true if the task can be processed. false if it is invalid or has completed..
     */
    private boolean isTaskIncomplete(@Nonnull Task task) {

        boolean taskReady = false;
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

    /**
     * Get all {@link Task}s from the database which have not yet completed fully.
     */
    private void fetchPendingTasks() {
        try {
            List<Task> tasks = mEbeanServer.find(Task.class)
                    .orderBy("id")
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
                    mPendingTaskQueue.add(task);
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
        if (!mTaskConsumerThread.isAlive() || mTaskConsumerThread.isInterrupted()) {
            mLog.d(TAG, "Starting the TaskQueue consumer thread.");

            // Get outstanding incomplete message tasks and start queue consumer thread..
            fetchPendingTasks();
            mTaskConsumerThread.start();
        }
    }

    /**
     * Add a new task to the TaskQueue (don't add tasks that are already saved).
     *
     * @param task task to save and add to TaskQueue.
     */
    @SuppressWarnings("WeakerAccess")
    public void addTask(@Nonnull Task task, TaskMessageResponse taskMessageResponse) {
        mLog.d(TAG, "Saving new task to TaskQueue and datastore.");

        if (isTaskIncomplete(task)) {
            // Save the task entry and put it in the queue.
            mPendingTaskQueue.add(task);

            // Add the listener to the map to be called later.
            if (taskMessageResponse != null) {
                mTaskIdClientListeners.put(task.id, taskMessageResponse);
            }
        }
    }

    /**
     * Updates or Inserts a task in the TaskQueue datastore.
     *
     * @param task the task to persist.
     */
    private boolean updateSaveTask(@Nonnull Task task) {
        try {
            Task existingTask = null;
            if (task.id != null) {
                existingTask = mEbeanServer.find(Task.class)
                        .where()
                        .eq("id", task.id)
                        .findUnique();
            }

            // If the task was not found, insert a new task.
            if (existingTask != null) {
                task.id = existingTask.id;
                mEbeanServer.update(task);

            } else {
                mEbeanServer.save(task);
            }

        } catch (Exception e) {
            mLog.e(TAG, "Error saving new task state", e);
            return false;
        }

        // Return true if the task has an id (and so was saved)
        return task.id != null;
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
        private Task mTask = null;

        public MessageResponseListener(Task task) {
            mTask = task;
        }

        @Override
        public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {
            super.messageResult(message, result);

            if (message.recipients != null) {
                TaskMessageResponse taskCallback = mTaskIdClientListeners.get(mTask.id);
                mLog.d(TAG, String.format("200-OK from pushservices for message %d", message.id));
                mTask.lastAttempt = Calendar.getInstance();

                /*
                 * If the message (task) needs retrying, the Task is set as
                 * TaskState.STATE_PARTIALLY_PROCESSED.
                 *
                 * Set each recipient state as RecipientState.STATE_PARTIALLY_PROCESSED
                 * (unless we have reached the maximum amount of allowed retries, in which case set
                 * the TaskState.STATE_FAILED and each Recipient as RecipientState.STATE_FAILED.
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

                        try {
                            mPendingTaskQueue.put(mTask);

                        } catch (InterruptedException e) {
                            mLog.w(TAG, "New Task not added. TaskQueue consumer was shutdown.");
                        }

                    } else {
                        // If the max retry count was reached, complete the task, and fail remaining recipients.
                        mTask.state = TaskState.STATE_FAILED;

                        for (Recipient messageRecipient : message.recipients) {
                            if (messageRecipient.state == RecipientState.STATE_WAITING_RETRY) {
                                messageRecipient.state = RecipientState.STATE_FAILED;
                                messageRecipient.failure = new RecipientFailure(PlatformFailureType.ERROR_TOO_MANY_RETRIES);
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
                if (!result.getUpdatedRegistrations().isEmpty() && taskCallback != null) {
                    taskCallback.updateRecipients(result.getUpdatedRegistrations());
                }

                // Send back any recipients to be deleted.
                if (!result.getStaleRecipients().isEmpty() && taskCallback != null) {
                    List<Recipient> staleRecipients = new ArrayList<>();

                    for (Recipient staleRecipient : result.getStaleRecipients()) {
                        staleRecipients.add(staleRecipient);
                    }
                    taskCallback.removeRecipients(staleRecipients);
                }

                if (taskCallback != null) {
                    taskCallback.completed(mTask);
                    removeClientListener(taskCallback);
                }

                // Update the task.
                updateSaveTask(mTask);
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, PushFailCause failCause) {
            TaskMessageResponse taskCallback = mTaskIdClientListeners.get(mTask.id);

            // Set the task as failed.
            mTask.state = TaskState.STATE_FAILED;

            if (taskCallback != null) {
                taskCallback.failed(mTask, failCause);
                removeClientListener(taskCallback);
            }

            // Update the task.
            updateSaveTask(mTask);

            mLog.e(TAG, String.format("Failed. %s response from push service for message %d",
                    failCause.name(), message.id));
        }

        /**
         * Remove a client's {@link TaskMessageResponse} listener if it's not null and
         * the {@link Task} has completed or failed.
         */
        private void removeClientListener(@Nonnull TaskMessageResponse taskMessageResponse) {
            if (mTask.state == TaskState.STATE_COMPLETE ||
                    mTask.state == TaskState.STATE_FAILED) {
                mTaskIdClientListeners.remove(mTask.id);
            }
        }
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
                // Loop through the tasks infinitely.
                while (true) {
                    // Sleep every so often so the serve isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                    mLog.d(TAG, "TaskQueue processing received task");
                    Task task = mPendingTaskQueue.take();
                    String taskName = task.name;

                    boolean backoffTimeReached = task.nextAttempt == null ||
                            task.nextAttempt.before(Calendar.getInstance());

                    // Backoff time was not reached.
                    if (!backoffTimeReached) {
                        mPendingTaskQueue.add(task);
                        mLog.d(TAG, String.format("Task %s hasn't reached backoff.", taskName));

                        // Task is ready for dispatch.
                    } else if (isTaskIncomplete(task)) {
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

                        // Save the updated task.
                        updateSaveTask(task);

                        // Dispatch each message within the task..
                        for (Message message : task.messages) {
                            if (message.recipients != null && !message.recipients.isEmpty()) {
                                mGcmDispatcher.dispatchMessageAsync(message, new MessageResponseListener(task));
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                mLog.d(TAG, "TaskQueue consumer thread was interrupted.");
            }
        }
    }
}
