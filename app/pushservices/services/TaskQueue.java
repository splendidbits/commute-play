package pushservices.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import pushservices.dao.TasksDao;
import pushservices.enums.FailureType;
import pushservices.enums.RecipientState;
import pushservices.exceptions.TaskValidationException;
import pushservices.helpers.TaskHelper;
import pushservices.interfaces.PlatformResponseCallback;
import pushservices.interfaces.RecipientPlatformMessageResponse;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton class that handles all Push Service task jobs.
 */
@Singleton
@SuppressWarnings({"WeakerAccess", "Convert2streamapi"})
public class TaskQueue {
    // Delay each taskqueue polling to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 250;
    private static final int TASKQUEUE_MAX_SIZE = 500;

    private ConcurrentHashMap<Long, PlatformResponseCallback> mTaskClientListeners = new ConcurrentHashMap<>();

    // TaskQueue instances.
    private ArrayBlockingQueue<Task> mPendingTaskQueue = new ArrayBlockingQueue<>(TASKQUEUE_MAX_SIZE);
    private QueueConsumer mQueueConsumer = new QueueConsumer();
    private Thread mTaskConsumerThread = new Thread(mQueueConsumer);

    @Inject
    private GcmMessageDispatcher mGcmMessageDispatcher;

    @Inject
    private TasksDao mTasksDao;

    /**
     * Private constructor to disallow instantiation.
     */
    private TaskQueue() {

    }

    /**
     * Privately instantiate the TaskQueue with required Dependencies.
     *
     * @param tasksDao             Task persistence.
     * @param gcmMessageDispatcher GCM Google message dispatcher.
     */
    TaskQueue(TasksDao tasksDao, GcmMessageDispatcher gcmMessageDispatcher) {
        mTasksDao = tasksDao;
        mGcmMessageDispatcher = gcmMessageDispatcher;
    }

    /**
     * Checks that the task consumer process is active and running, and starts the
     * TaskQueue {@link Task} polling process if it is not.
     */
    public void start() {
        if (mTaskConsumerThread == null || !mTaskConsumerThread.isAlive()) {
            Logger.debug("Starting the TaskQueue process.");
            mTaskConsumerThread.start();
            queuePendingTasks();
        }
    }

    /**
     * Checks that the task consumer process is active and running, and starts the
     * TaskQueue {@link Task} polling process if it is not.
     */
    public void stop() {
        if (mTaskConsumerThread != null && mTaskConsumerThread.isAlive()) {
            Logger.debug("Stopping the TaskQueue Consumer process.");
            mPendingTaskQueue = null;
        }
    }

    /**
     * Queue any tasks that have pending recipients. If you call this method, be sure that the tasks
     * in question are not being processed by the ConsumerThread, or are awaiting results
     * from the dispatcher. Ideally, the TaskQueue should not be started.
     */
    private void queuePendingTasks() {
        if (mPendingTaskQueue != null) {

            // Get outstanding incomplete message tasks and start queue consumer thread..
            List<Task> pendingTasks = mTasksDao.fetchPendingTasks();
            for (Task task : pendingTasks) {
                mPendingTaskQueue.add(task);
            }
        }
    }

    /**
     * Dispatch a Task {@link Message}
     *
     * @param task     The {@link Task} to dispatch.
     * @param callback The {@link PlatformResponseCallback} callback which notifies on required actions.
     *                 <p>
     *                 If there was a problem with the task a PlatformTaskException will be thrown.
     */
    private void dispatchTask(@Nonnull Task task, @Nonnull PlatformMessageCallback callback)
            throws TaskValidationException {
        Calendar currentTime = Calendar.getInstance();

        // Verify the Task has all required attributes.
        TaskHelper.verifyTask(task);

        // Record whether any of the messages in the task were dispatched, as non-dispatched
        // tasks should be re-queued.
        boolean taskDispatched = false;

        //noinspection ConstantConditions
        for (Message message : task.messages) {

            int messageRecipientCount = 0;
            if (message.recipients != null) {

                // Set recipient states to processing for ready recipients.
                for (Recipient recipient : message.recipients) {

                    // Fail the recipient if it doesn't contain a token.
                    if (recipient.token == null || recipient.token.isEmpty()) {
                        recipient.state = RecipientState.STATE_FAILED;
                        continue;
                    }

                    // The recipient has either already failed or completed.
                    if (TaskHelper.isRecipientStatePending(recipient)) {

                        // The recipient is not in the cooling off period.
                        if (!TaskHelper.isRecipientCoolingOff(recipient)) {
                            recipient.previousAttempt = currentTime;
                            recipient.state = RecipientState.STATE_PROCESSING;
                            messageRecipientCount += 1;

                        } else {
                            // If the recipient is within the back-off period.
                            recipient.state = RecipientState.STATE_WAITING_RETRY;
                        }
                    }
                }
                // Update the task message in the database.
                mTasksDao.updateMessage(message);
            }

            // If there are pendingRecipients, dispatch the entire task.
            if (messageRecipientCount > 0) {
                Logger.debug(String.format("Dispatching message to %d recipients", messageRecipientCount));
                task.lastUpdated = Calendar.getInstance();
                mGcmMessageDispatcher.dispatchMessage(message, callback);

                // A message in the task had recipients so was dispatched.
                taskDispatched = true;
            }
        }

        // If the task was not dispatched, re-add task to tail of queue.
        if (!taskDispatched) {
            mPendingTaskQueue.add(task);
        }
    }

    /**
     * Add a new, unsaved task to the TaskQueue.
     *
     * @param task      task to add to TaskQueue and dispatch.
     * @param immediate true if the TaskQueue process should be skipped and the message
     *                  just sent immediately.
     */
    public void enqueueTask(@Nonnull Task task, @Nonnull PlatformResponseCallback callback, boolean immediate)
            throws TaskValidationException {
        Logger.debug("Relieved client task.");

        Task clonedTask = TaskHelper.copyTask(task);
        Task existingTask = mTasksDao.fetchTask(task.id);

        if (existingTask != null) {
            throw new TaskValidationException("Task or Task children are already persisted.\n" +
                    "Ensure that all children have a unique or null id");
        }

        boolean taskPersisted = mTasksDao.insertTask(clonedTask);
        if (!taskPersisted) {
            throw new TaskValidationException("Error saving Task into persistence.");
        }

        // Add the listener to the map to be called later, and queue task.
        mTaskClientListeners.put(clonedTask.id, callback);

        // Queue the task or send it immediately.
        if (immediate) {
            dispatchTask(task, new PlatformMessageCallback(task));

        } else if (mPendingTaskQueue != null) {
            mPendingTaskQueue.add(task);
        }
    }

    /**
     * Fail all recipients in all messages for a given task.
     *
     * @param task the task to fail.
     */
    private void failTask(@Nonnull Task task) {
        if (task.id != null && task.messages != null) {
            for (Message message : task.messages) {
                if (message.recipients != null) {
                    for (Recipient recipient : message.recipients) {
                        recipient.state = RecipientState.STATE_FAILED;
                    }
                }
            }
            mTasksDao.updateTask(task);
        }
    }

    /**
     * Response back from the push message pushservices (APNS or GCM) for the sent message.
     * The message may have either succeeded or failed.
     * <p>
     * As {@link RecipientPlatformMessageResponse} is being extended, instead of simply implementing
     * the {@link RecipientPlatformMessageResponse} callback, the {@link Recipient}s are
     * sorted back into the original sent message.
     */
    private class PlatformMessageCallback extends RecipientPlatformMessageResponse {
        private Task mTask = null;

        public PlatformMessageCallback(@Nonnull Task task) {
            mTask = task;
        }

        @Override
        public void messageResult(@NotNull Message message, @Nonnull MessageResult result) {
            super.messageResult(message, result);

            // Update the message entry.
            mTasksDao.updateMessage(message);

            if (message.recipients != null) {
                Logger.debug("200-OK from push-services for message");

                // Add the task to the back of the queue.
                PlatformResponseCallback taskCallback = mTaskClientListeners.get(mTask.id);
                if (!result.getRecipientsToRetry().isEmpty()) {
                    mPendingTaskQueue.add(mTask);
                }

                // Send back any recipients to be updated.
                if (!result.getUpdatedRegistrations().isEmpty() && taskCallback != null) {
                    taskCallback.updateRecipients(result.getUpdatedRegistrations());
                }

                // Send back any recipients to be deleted.
                if (!result.getFailedRecipients().isEmpty() && taskCallback != null) {
                    taskCallback.removeRecipients(result.getFailedRecipients().keySet());
                }

                if (taskCallback != null) {
                    mTaskClientListeners.remove(mTask.id, taskCallback);
                    taskCallback.completed(mTask);
                }
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, FailureType failureType) {
            Logger.error(String.format("%s response from push service for task %s", failureType.name(), mTask.name));

            // Update the message entry.
            mTasksDao.updateMessage(message);

            PlatformResponseCallback taskCallback = mTaskClientListeners.get(mTask.id);
            if (taskCallback != null) {
                taskCallback.failed(mTask, failureType);
                mTaskClientListeners.remove(mTask.id, taskCallback);
            }
        }
    }

    /**
     * Thread that loops through the blocking queue and sends
     * tasks to the platform push provider. This will block if there are no tasks to take,
     * hence the Runnable.
     */
    @SuppressWarnings("ConstantConditions")
    private class QueueConsumer implements Runnable {

        @Override
        public void run() {
            try {
                while (mPendingTaskQueue != null) {
                    // Sleep on while so often so the server isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                    // Take and remove the task from queue.
                    Task task = mPendingTaskQueue.take();
                    Logger.debug("TaskQueue processing received task");

                    // Dispatch the queued task on the main thread.
                    try {
                        dispatchTask(task, new PlatformMessageCallback(task));

                    } catch (TaskValidationException e) {
                        Logger.debug("A Task was corrupt and threw a PlatformTaskException.");
                        failTask(task);
                    }
                }

            } catch (InterruptedException e) {
                Logger.debug("TaskQueue consumer thread was interrupted.");
            }
        }
    }
}
