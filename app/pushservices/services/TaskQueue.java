package pushservices.services;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import services.splendidlog.Logger;
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
    private static final String TAG = TaskQueue.class.getSimpleName();

    // Delay each taskqueue polling to a 1/4 second so the server isn't flooded.
    private static final long TASKQUEUE_POLL_INTERVAL_MS = 250;
    private static final int TASKQUEUE_MAX_SIZE = 500;

    private ConcurrentHashMap<Long, PlatformResponseCallback> mTaskClientListeners = new ConcurrentHashMap<>();

    // TaskQueue instances.
    private ArrayBlockingQueue<Task> mPendingTaskQueue = new ArrayBlockingQueue<>(TASKQUEUE_MAX_SIZE);
    private QueueConsumer mQueueConsumer = new QueueConsumer();
    private Thread mTaskConsumerThread = new Thread(mQueueConsumer);

    private GoogleMessageDispatcher mGoogleMessageDispatcher;
    private TasksDao mTasksDao;

    /**
     * Privately instantiate the TaskQueue with required Dependencies.
     *
     * @param tasksDao Task persistence.
     * @param googleMessageDispatcher GCM Google message dispatcher.
     */
    TaskQueue(TasksDao tasksDao, GoogleMessageDispatcher googleMessageDispatcher) {
        mTasksDao = tasksDao;
        mGoogleMessageDispatcher = googleMessageDispatcher;
    }

    /**
     * Private constructor to disallow instantiation.
     */
    private TaskQueue() {
    }

    /**
     * Checks that the task consumer thread is active and running, and starts the
     * TaskQueue {@link Task} polling process if it is not.
     */
    public void start() {
        if (mTaskConsumerThread == null || !mTaskConsumerThread.isAlive()) {
            Logger.debug("Starting the TaskQueue consumer thread.");
            mTaskConsumerThread.start();

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
        TaskHelper.verifyTask(task);

        // Task is ready for dispatch.
        task.lastUpdated = Calendar.getInstance();

        //noinspection ConstantConditions
        for (Message message : task.messages) {
            int recipientsCount = 0;

            if (message.recipients != null) {

                // Set recipient states to processing for ready recipients.
                for (Recipient recipient : message.recipients) {

                    if (TaskHelper.isRecipientReady(recipient)) {
                        recipient.state = RecipientState.STATE_PROCESSING;
                        recipientsCount += 1;

                    } else {
                        recipient.state = RecipientState.STATE_WAITING_RETRY;
                    }
                }
            }

            if (recipientsCount > 0) {
                Logger.debug(String.format("Dispatching message to %d recipients", recipientsCount));
                mGoogleMessageDispatcher.dispatchMessage(message, callback);

            } else {
                mPendingTaskQueue.add(task);
            }
        }

        // Update the task message in the database.
        mTasksDao.updateTask(task);
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
                Logger.debug("200-OK from pushservices for message");


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
            Logger.error(TAG, String.format("%s response from push service for task %s", failureType.name(), mTask.name));
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
                    Logger.debug("TaskQueue processing received task");

                    // Sleep on while so often so the server isn't hammered.
                    Thread.sleep(TASKQUEUE_POLL_INTERVAL_MS);

                    // Take and remove the task from queue.
                    Task task = mPendingTaskQueue.take();

                    // Get the clientCallback listener for task if it is available.
                    dispatchTask(task, new PlatformMessageCallback(task));
                }

            } catch (InterruptedException e) {
                Logger.debug("TaskQueue consumer thread was interrupted.");

            } catch (TaskValidationException e) {
                Logger.debug("A Task was corrupt and threw a PlatformTaskException.");
                start();
            }
        }
    }
}
