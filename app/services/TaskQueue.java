package services;

import com.avaje.ebean.EbeanServer;
import interfaces.MessageResponseListener;
import main.Log;
import models.app.MessageResult;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.RecipientFailure;
import models.taskqueue.Task;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * The singleton class that handles all GCM task jobs.
 * <p>
 * TODO: Move database related functions to a DAO.
 */
@Singleton
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private static final int MAXIMUM_TASK_RETRIES = 10;
    private static boolean mQueueRunning = false;
    private List<Task> mPendingTasks = Collections.synchronizedList(new ArrayList<>());

    private AccountService mAccountService;
    private GcmDispatcher mGcmDispatcher;
    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public TaskQueue(EbeanServer ebeanServer, Log log, GcmDispatcher gcmDispatcher, AccountService accountService) {
        mEbeanServer = ebeanServer;
        mLog = log;
        mGcmDispatcher = gcmDispatcher;
        mAccountService = accountService;

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
                Iterator<Task> pendingTaskIterator = mPendingTasks.iterator();
                // Do not use the tasks which are running or have failed.
                while (pendingTaskIterator.hasNext()) {
                    Task taskToProcess = pendingTaskIterator.next();
                    if (isTaskIncomplete(taskToProcess)) {

                        // Create a single response class for each separate message in the task.
                        DispatchResponseListener taskDispatchResponse = new DispatchResponseListener(taskToProcess);
                        for (Message message : taskToProcess.messages) {

                            // Set the task as processing
                            taskToProcess.processState = Task.ProcessState.PROCESSING;
                            updateTask(taskToProcess);

                            // Dispatch the message.
                            mGcmDispatcher.dispatchMessageAsync(message,
                                    taskDispatchResponse,
                                    message.platformAccount);
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
    private boolean isTaskIncomplete(@Nonnull Task task) {
        if (!task.processState.equals(Task.ProcessState.COMPLETE) &&
                !task.processState.equals(Task.ProcessState.FAILED)) {
            return true;
        }
        return false;
    }

    /**
     * Get all tasks from the database which have not yet completed fully.
     */
    private void fetchPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .where()
                .disjunction()
                .ne("processState", Task.ProcessState.FAILED)
                .ne("processState", Task.ProcessState.COMPLETE)
                .endJunction()
                .filterMany("messages.recipients").ne("state", Recipient.ProcessState.COMPLETE)
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
    public synchronized boolean addTask(@Nonnull Task task) {
        // Check to see if the task is already in the queue.
        for (Task pendingTask : mPendingTasks) {
            if (pendingTask.taskId.equals(task.taskId)) {
                return false;
            }
        }

        mLog.d(TAG, "Saving new task to TaskQueue and datastore.");
        mPendingTasks.add(task);
        mEbeanServer.beginTransaction();

        try {
            mEbeanServer.save(task);
            mEbeanServer.commitTransaction();

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task to database", exception);
            mEbeanServer.rollbackTransaction();
            return false;

        } finally {
            mEbeanServer.endTransaction();
        }

        return true;
    }

    /**
     * Updates a task in TaskQueue with a new processing state.
     *
     * @param task the task within queue
     */
    private void updateTask(@Nonnull Task task) {
        if (task.taskId != null) {
            Iterator<Task> pendingTaskIterator = mPendingTasks.iterator();
            while (pendingTaskIterator.hasNext()) {

                Task pendingTask = pendingTaskIterator.next();
                if (pendingTask.taskId.equals(task.taskId)) {

                    pendingTaskIterator.remove();
                    if (isTaskIncomplete(task)) {
                        mPendingTasks.add(task);
                    }
                }
            }

            mEbeanServer.beginTransaction();
            try {
                mEbeanServer.save(task);
                mEbeanServer.commitTransaction();

            } catch (Exception exception) {
                mLog.e(TAG, "Error saving new task state", exception);

            } finally {
                mEbeanServer.endTransaction();
            }
        }
    }

    /**
     * Results from the dispatcher for individual tasks of one or
     * more messages.
     */
    private class DispatchResponseListener implements MessageResponseListener {
        private Task mOriginalTask = null;

        DispatchResponseListener(Task originalTask) {
            mOriginalTask = originalTask;
        }

        @Override
        public void messageResult(@NotNull @Nonnull MessageResult result) {
            Message sentMessage = result.getOriginalMessage();
            int taskRetryCount = mOriginalTask.retryCount;

            mLog.d(TAG, String.format("Successful response from Google for message %d", sentMessage.messageId));

            Map<Recipient, String> failedRecipients = result.getRecipientErrors();
            List<Recipient> successfulRecipients = result.getSuccessfulRecipients();
            List<Recipient> retryRecipients = result.getRecipientsToRetry();
            List<Recipient> staleRecipients = result.getStaleRecipients();

            if (!successfulRecipients.isEmpty() && failedRecipients.isEmpty()) {
                mOriginalTask.processState = Task.ProcessState.COMPLETE;

            } else if (result.hasCriticalErrors()) {
                mOriginalTask.processState = Task.ProcessState.FAILED;
            }

            // Update the failed recipients with the reason why.
            for (Map.Entry<Recipient, String> entry : failedRecipients.entrySet()) {
                Recipient failedRecipient = entry.getKey();
                String failureReason = entry.getValue();

                failedRecipient.mRecipientFailure = new RecipientFailure(failureReason);
                failedRecipient.state = Recipient.ProcessState.COMPLETE;
                failedRecipient.save();
            }

            // Remove stale registrations from the datastore.
            // TODO: Add an interface so there is no hard link between GCM module -> main module.
            for (Recipient staleRecipient : staleRecipients) {
                mAccountService.deleteRegistration(staleRecipient.token);
            }

            // If we are retrying for some recipients, then update the task accordingly.
            if (!retryRecipients.isEmpty()) {
                if (taskRetryCount <= MAXIMUM_TASK_RETRIES) {
                    mOriginalTask.retryCount = taskRetryCount + 1;
                    mOriginalTask.lastAttempt = Calendar.getInstance();

                    // Exponentially bump up the next retry interval
                    Calendar nextAttemptCal = Calendar.getInstance();
                    nextAttemptCal.add(Calendar.MINUTE, (mOriginalTask.retryCount * 2));
                    mOriginalTask.nextAttempt = nextAttemptCal;

                    mOriginalTask.processState = Task.ProcessState.PARTIALLY_COMPLETE;
                    for (Recipient recipientToRetry : sentMessage.recipients) {
                        recipientToRetry.state = Recipient.ProcessState.RETRY;
                        recipientToRetry.save();
                    }

                } else {
                    // If the maximum retry count has eben reached, complete the task, and fail the remaining recipients.
                    mOriginalTask.processState = Task.ProcessState.COMPLETE;

                    for (Recipient recipientToRetry : sentMessage.recipients) {
                        recipientToRetry.state = Recipient.ProcessState.COMPLETE;
                        recipientToRetry.mRecipientFailure = new RecipientFailure("MaxRetryReached");
                        recipientToRetry.save();
                    }
                }
            }
            updateTask(mOriginalTask);
        }

        @Override
        public void messageFailed(@Nonnull Message message, HardFailCause failCause) {
            mLog.e(TAG, String.format("Failed. %s response from Google for message %d",
                    failCause.name(), message.messageId));

            mOriginalTask.processState = Task.ProcessState.FAILED;
            updateTask(mOriginalTask);
        }
    }
}
