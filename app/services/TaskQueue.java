package services;

import com.avaje.ebean.EbeanServer;
import interfaces.IPushResponse;
import main.Log;
import models.app.MessageResult;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.Task;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The singleton class that handles all GCM task jobs.
 *
 * TODO: Move database related functions to a DAO.
 */
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private static boolean mQueueRunning = false;
    private List<Task> mPendingTasks = Collections.synchronizedList(new ArrayList<>());

    private GcmDispatcher mGcmDispatcher;
    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public TaskQueue(EbeanServer ebeanServer, Log log, GcmDispatcher gcmDispatcher) {
        mEbeanServer = ebeanServer;
        mLog = log;
        mGcmDispatcher = gcmDispatcher;

        // Fetch any tasks that are partially or incomplete.
        fetchPendingTasks();
    }

    /**
     * Run through the pending task queue and pick up / send messages
     * to dispatcher.
     */
    private synchronized void runQueue() {
        if (!mQueueRunning) {
            mQueueRunning = true;

            try {
                List<Task> currentTaskList = new ArrayList<>(mPendingTasks);
                if (!currentTaskList.isEmpty()) {

                    // Do not use the tasks which are running or have failed.
                    for (Task taskToProcess : currentTaskList) {
                        if (isTaskReady(taskToProcess)) {

                            // Create a single response class for each separate message in the task.
                            DispatchResponse taskDispatchResponse = new DispatchResponse(taskToProcess);
                            for (Message message : taskToProcess.messages) {

                                // Dispatch the message.
                                mGcmDispatcher.dispatchMessageAsync(message,
                                        taskDispatchResponse,
                                        message.platformAccount);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                mLog.e(TAG, "Error processing entry in TaskQueue", e);

            } finally {
                mQueueRunning = false;
            }
        }
    }

    /**
     * Is the task ready to be dispatched (not waiting for next interval and hasn't
     * failed or isn't currently processing)
     *
     * @param task The task to start work on.
     * @return true or false, duh.
     */
    private boolean isTaskReady(@Nonnull Task task) {
        if (!task.mTaskProcessState.equals(Task.TaskProcessState.STATE_COMPLETE) &&
                !task.mTaskProcessState.equals(Task.TaskProcessState.STATE_PERMANENTLY_FAILED)) {
            return true;
        }
        return false;
    }

    /**
     * Get all tasks from the database which have not yet completed fully.
     */
    private void fetchPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .fetch("recipients")
                .where()
                .disjunction()
                .eq("processState", Task.TaskProcessState.STATE_NOT_STARTED)
                .eq("processState", Task.TaskProcessState.STATE_PARTIALLY_COMPLETE)
                .eq("processState", Task.TaskProcessState.STATE_PROCESSING)
                .endJunction()
                .where()
                .disjunction()
                .eq("recipients.recipientState", Recipient.RecipientProcessState.STATE_NOT_STARTED)
                .eq("recipients.recipientState", Recipient.RecipientProcessState.STATE_PROCESSING)
                .endJunction()
                .findList();

        // Add the saved pending tasks back into the queue
        if (tasks != null) {
            for (Task task : tasks) {
                mPendingTasks.add(task);
            }
        }
    }

    /**
     * Run through all the jobs that TaskQueue needs to perform. Should be fired
     * externally through a system cronjob or a platform services such as Akka.
     */
    public void sweep() {
        runQueue();
        mLog.d(TAG, "Performing a sweep of TaskQueue.");
    }

    /**
     * Add a new task to the TaskQueue (don't add tasks that are already saved).
     *
     * @param task task to save and add to TaskQueue.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean addTask(@Nonnull Task task) {
        mLog.d(TAG, "Saving new task to database");
        mEbeanServer.beginTransaction();

        try {
            mEbeanServer.insert(task);
            mEbeanServer.commitTransaction();

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task to database", exception);
            mEbeanServer.rollbackTransaction();

            return false;

        } finally {
            mEbeanServer.endTransaction();
        }

        mLog.d(TAG, "Adding new task to queue");
        mPendingTasks.add(task);

        return true;
    }

    /**
     * Updates a task in TaskQueue with a new processing state.
     *
     * @param task  the task within queue
     * @param state the new pending state.
     */
    private void updateTaskState(@Nonnull Task task, @Nonnull Task.TaskProcessState state) {

        // Fetch the sent task out of the pending queue.
        for (Task foundTask : mPendingTasks) {
            if (foundTask.taskId.equals(task.taskId)) {
                foundTask.mTaskProcessState = state;
            }
        }

        task.mTaskProcessState = state;
        mEbeanServer.createTransaction();
        try {
            mEbeanServer.save(task);
            mEbeanServer.commitTransaction();

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task state", exception);

        } finally {
            mEbeanServer.endTransaction();
        }
    }

    /**
     * Results from the dispatcher for individual tasks of one or
     * more messages.
     */
    private class DispatchResponse implements IPushResponse {
        private Task mOriginalTask = null;

        DispatchResponse(Task originalTask) {
            mOriginalTask = originalTask;
            updateTaskState(originalTask, Task.TaskProcessState.STATE_PROCESSING);
        }

        @Override
        public void messageResult(@NotNull @Nonnull MessageResult result) {
            Message sentMessage = result.getOriginalMessage();
            mLog.d(TAG, String.format("Successful response from Google for message %d", sentMessage.messageId));

            if (result.getFailCount() == 0 && result.getSuccessCount() > 0) {
                updateTaskState(mOriginalTask, Task.TaskProcessState.STATE_COMPLETE);

            } else if (result.hasCriticalErrors()) {
                updateTaskState(mOriginalTask, Task.TaskProcessState.STATE_PERMANENTLY_FAILED);
            }
        }

        @Override
        public void messageFailed(@Nonnull Message message, HardFailCause failCause) {
            mLog.e(TAG, String.format("Failed. %s response from Google for message %d",
                    failCause.name(), message.messageId));
        }
    }
}
