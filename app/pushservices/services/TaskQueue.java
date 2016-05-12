package pushservices.services;

import org.jetbrains.annotations.NotNull;
import pushservices.dao.TasksDao;
import pushservices.enums.FailureType;
import pushservices.enums.RecipientState;
import pushservices.exceptions.TaskValidationException;
import pushservices.helpers.TaskHelper;
import pushservices.interfaces.RecipientPlatformMessageResponse;
import pushservices.interfaces.PlatformResponseCallback;
import pushservices.models.app.MessageResult;
import pushservices.models.database.Message;
import pushservices.models.database.Recipient;
import pushservices.models.database.Task;
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton class that handles all Push Service task jobs.
 */
@SuppressWarnings({"WeakerAccess", "Convert2streamapi"})
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();

    // Delay each taskqueue polling to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 250;
    private static final int TASKQUEUE_MAX_SIZE = 250;

    private ArrayBlockingQueue<Task> mPendingTaskQueue = new ArrayBlockingQueue<>(TASKQUEUE_MAX_SIZE);
    private ConcurrentHashMap<Long, PlatformResponseCallback> mTaskClientListeners = new ConcurrentHashMap<>();
    private QueueConsumer mQueueConsumer = new QueueConsumer();
    private Thread mTaskConsumerThread;

    @Inject
    private GoogleMessageDispatcher mGoogleMessageDispatcher;

    @Inject
    private TasksDao mTasksDao;

    @Inject
    private Log mLog;

    @Inject
    public TaskQueue() {
    }

    /**
     * Checks that the task consumer thread is active and running, and starts the TaskQueue
     * {@link Task} polling process if it is not.
     */
    public void start() {
        if (mTaskConsumerThread == null || !mTaskConsumerThread.isAlive()) {
            mLog.d(TAG, "Starting the TaskQueue consumer thread.");
            mTaskConsumerThread = new Thread(mQueueConsumer);

            // Get outstanding incomplete message tasks and start queue consumer thread..
            List<Task> pendingTasks = mTasksDao.fetchPendingTasks();
            for (Task task : pendingTasks) {
                mPendingTaskQueue.add(task);
            }
            mTaskConsumerThread.start();
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
        TaskHelper.verifyTask(task);

        // Task is ready for dispatch.
        task.lastUpdated = Calendar.getInstance();

        //noinspection ConstantConditions
        for (Message message : task.messages) {
            int recipientsCount = 0;

            // Set recipient states to processing for ready recipients.
            if (message.recipients != null) {
                for (Recipient recipient : message.recipients) {

                    if (TaskHelper.isRecipientReady(recipient)) {
                        recipient.state = RecipientState.STATE_PROCESSING;
                        recipientsCount += 1;

                    } else {
                        recipient.state = RecipientState.STATE_WAITING_RETRY;
                    }
                }
            }

            // Update the task message in the database.
            mTasksDao.updateMessage(message);

            if (recipientsCount > 0) {
                mLog.d(TAG, String.format("Dispatching message to %d recipients", recipientsCount));
                mGoogleMessageDispatcher.dispatchMessage(message, callback);

            } else {
                mPendingTaskQueue.add(task);
            }
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
        mLog.d(TAG, "Relieved client task.");

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
        } else {
            mPendingTaskQueue.add(task);
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

            if (message.recipients != null) {
                PlatformResponseCallback taskCallback = mTaskClientListeners.get(mTask.id);
                mLog.d(TAG, "200-OK from pushservices for message");


                // Add the task to the back of the queue.
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
                    taskCallback.completed(mTask);
                    mTaskClientListeners.remove(mTask.id, taskCallback);
                }

                // Save the updated task.
                mTasksDao.updateTask(mTask);
            }
        }

        @Override
        public void messageFailure(@Nonnull Message message, FailureType failureType) {
            PlatformResponseCallback taskCallback = mTaskClientListeners.get(mTask.id);

            if (taskCallback != null) {
                taskCallback.failed(mTask, failureType);
                mTaskClientListeners.remove(mTask.id, taskCallback);
            }

            // Save the updated task.
            mTasksDao.updateTask(mTask);
            mLog.e(TAG, String.format("%s response from push service for task %s", failureType.name(), mTask.name));
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
                // Loop through the tasks infinitely.
                while (mPendingTaskQueue != null) {
                    mLog.d(TAG, "TaskQueue processing received task");

                    // Sleep on while so often so the server isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                    // Take and remove the task from queue.
                    Task task = mPendingTaskQueue.take();

                    // Get the clientCallback listener for task if it is available.
                    dispatchTask(task, new PlatformMessageCallback(task));
                }

            } catch (InterruptedException e) {
                mLog.d(TAG, "TaskQueue consumer thread was interrupted.");

            } catch (TaskValidationException e) {
                mLog.d(TAG, "A Task was corrupt and threw a PlatformTaskException.");
                start();
            }
        }
    }
}
