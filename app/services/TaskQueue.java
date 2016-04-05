package services;

import com.avaje.ebean.EbeanServer;
import interfaces.IPushResponse;
import main.Log;
import models.app.MessageResult;
import models.taskqueue.Message;
import models.taskqueue.Task;
import services.gcm.GcmDispatcher;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The singleton class that handles all GCM task jobs.
 */
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private boolean mQueueRunning = false;
    private List<Task> mPendingTasks = Collections.synchronizedList(new ArrayList<>());

    @Inject
    private EbeanServer mEbeanServer;

    @Inject
    private Log mLog;

    @Inject
    public TaskQueue(EbeanServer ebeanServer, Log log) {
        mEbeanServer = ebeanServer;
        mLog = log;
    }

    /**
     * Run through the pending task queue and pick up / send messages
     * to dispatcher.
     */
    private synchronized void runQueue() {
        if (!mQueueRunning) {
            try {
                List<Task> currentTaskList = new ArrayList<>(mPendingTasks);

                mQueueRunning = true;
                while (currentTaskList.iterator().hasNext()) {
                    Task task = currentTaskList.iterator().next();

                    // Do not use the task if it is running or complete or failed.
                    if (!task.processState.equals(Task.ProcessState.STATE_COMPLETE) &&
                            !task.processState.equals(Task.ProcessState.STATE_PERMANENTLY_FAILED) &&
                            !task.processState.equals(Task.ProcessState.STATE_PROCESSING)) {

                        // Create a single response class for each separate message in the task.
                        DispatchResponse taskDispatchResponse = new DispatchResponse(task);
                        for (Message taskMessage : task.messages) {
                            new GcmDispatcher(taskMessage, taskDispatchResponse);
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
     * Set each task that is currently in rotation to not be.
     */
    private void fetchPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .where()
                .ne("processState", Task.ProcessState.STATE_COMPLETE)
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
    public boolean addTask(@Nonnull Task task) {
        mLog.d(TAG, "Saving new task to database");
        try {
            mEbeanServer.insert(task);
            task.refresh();

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task to database", exception);
            return false;
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
    private void updateTaskState(@Nonnull Task task, @Nonnull Task.ProcessState state) {
        Iterator<Task> pendingTaskIterator = mPendingTasks.iterator();

        // Fetch the sent task out of the pending queue.
        while (pendingTaskIterator.hasNext()) {
            Task foundTask = pendingTaskIterator.next();
            if (foundTask.taskId.equals(task.taskId)) {
                foundTask.processState = state;
            }
        }

        task.processState = state;
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
        private List<Message> mTaskMessages = null;

        public DispatchResponse(Task originalTask) {
            mOriginalTask = originalTask;
            mTaskMessages = originalTask.messages;
            updateTaskState(originalTask, Task.ProcessState.STATE_PROCESSING);
        }

        @Override
        public void messageSuccess(@Nonnull MessageResult result) {
            if (result.getFailCount() == 0 && result.getSuccessCount() > 0) {
                updateTaskState(mOriginalTask, Task.ProcessState.STATE_COMPLETE);

            } else if (result.hasCriticalErrors()) {
                updateTaskState(mOriginalTask, Task.ProcessState.STATE_PERMANENTLY_FAILED);
            }
        }

        @Override
        public void messageFailed(@Nonnull MessageResult result) {

        }
    }
}
