package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.Transaction;
import interfaces.MessageResponseListener;
import main.Log;
import models.app.MessageResult;
import models.app.ProcessState;
import models.taskqueue.Message;
import models.taskqueue.Recipient;
import models.taskqueue.RecipientFailure;
import models.taskqueue.Task;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

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

    private TransferQueue<Task> mPendingTaskQueue = new LinkedTransferQueue<>();
    private QueueConsumer mQueueConsumer = new QueueConsumer();

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

        // Start the queue take thread.
        new Thread(mQueueConsumer).start();
    }

    /**
     * Run through the pending task queue and pick up / send messages
     * to dispatcher.
     */
    private synchronized void runQueue() {
        if (!mQueueRunning) {
            mQueueRunning = true;

            try {
                // Do not use the tasks which are running or have failed.
                mQueueConsumer.run();

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
        Calendar currentTime = Calendar.getInstance();

        if (!task.processState.equals(ProcessState.STATE_COMPLETE) &&
                !task.processState.equals(ProcessState.STATE_FAILED) &&
                !task.nextAttempt.after(currentTime)) {
            return true;
        }
        return false;
    }

    /**
     * Get all tasks from the database which have not yet completed fully.
     */
    private void fetchPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .fetch("messages", new FetchConfig().query())
                .fetch("messages.recipients", new FetchConfig().query())
                .where()
                .conjunction()
                .ne("processState", ProcessState.STATE_FAILED)
                .ne("processState", ProcessState.STATE_COMPLETE)
                .endJunction()
                .filterMany("messages.recipients").ne("state", ProcessState.STATE_COMPLETE)
                .filterMany("messages.recipients").ne("state", ProcessState.STATE_FAILED)
                .findList();

        // Add the saved pending tasks back into the queue
        if (tasks != null) {
            for (Task task : tasks) {
                mPendingTaskQueue.offer(task);
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
        mLog.d(TAG, "Saving new task to TaskQueue and datastore.");
        if (!mPendingTaskQueue.contains(task)) {

            // Add to the queue in a new thread.
            if (task.processState == null ||
                    !task.processState.equals(ProcessState.STATE_COMPLETE) &&
                            !task.processState.equals(ProcessState.STATE_FAILED)) {
                CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        updateTask(task);
                        mPendingTaskQueue.offer(task);
                    }
                });
            }

            return true;
        }
        return false;
    }

    /**
     * Updates a task in TaskQueue with a new processing state.
     *
     * @param task the task within queue
     */

    private void updateTask(@Nonnull Task task) {
        Transaction taskTransaction = mEbeanServer.createTransaction();
        try {
            boolean updatedTask = false;
            if (task.taskId != null) {
                Task persistedTask = mEbeanServer
                        .find(Task.class)
                        .fetch("messages")
                        .fetch("messages.recipients")
                        .where()
                        .idEq(task.taskId)
                        .findUnique();

                if (persistedTask != null) {
                    updatedTask = true;
                    persistedTask.messages = task.messages;
                    persistedTask.processState = task.processState;
                    persistedTask.retryCount = task.retryCount;
                    persistedTask.lastAttempt = task.lastAttempt;
                    persistedTask.nextAttempt = task.nextAttempt;
                    persistedTask.taskAdded = task.taskAdded;
                    mEbeanServer.update(persistedTask, taskTransaction, false);
                }
            }

            if (!updatedTask) {
                mEbeanServer.save(task, taskTransaction);
            }
            taskTransaction.commit();

        } catch (Exception exception) {
            mLog.e(TAG, "Error saving new task state", exception);
            taskTransaction.rollback();

        } finally {
            taskTransaction.end();
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
            Message message = result.getOriginalMessage();
            int taskRetryCount = mOriginalTask.retryCount;

            mLog.d(TAG, String.format("Successful response from Google for message %d", message.messageId));

            Map<Recipient, String> failedRecipients = result.getRecipientErrors();
            List<Recipient> successfulRecipients = result.getSuccessfulRecipients();
            List<Recipient> retryRecipients = result.getRecipientsToRetry();
            List<Recipient> staleRecipients = result.getStaleRecipients();

            if (!successfulRecipients.isEmpty() && failedRecipients.isEmpty() && retryRecipients.isEmpty()) {
                // Everything was successful.

                mOriginalTask.processState = ProcessState.STATE_COMPLETE;
                for (Recipient recipient : message.recipients) {
                    recipient.state = ProcessState.STATE_COMPLETE;
                }

            } else if (result.hasCriticalErrors()) {
                // There were unrecoverable errors.
                mOriginalTask.processState = ProcessState.STATE_FAILED;

            } else {
                // Every other situation.

                // Update the FAILED RECIPIENTS (with the reason why)
                for (Map.Entry<Recipient, String> entry : failedRecipients.entrySet()) {

                    Recipient failedRecipient = entry.getKey();
                    String failureReason = entry.getValue();

                    failedRecipient.failure = new RecipientFailure(failureReason);
                    failedRecipient.state = ProcessState.STATE_FAILED;
                    mEbeanServer.save(failedRecipient);
                }

                // Remove stale registrations from the datastore.
                // TODO: Add an interface so there is no hard link between GCM module -> main module.
                for (Recipient staleRecipient : staleRecipients) {
                    mAccountService.deleteRegistration(staleRecipient.token);
                }

                // Retry logic.
                if (!retryRecipients.isEmpty()) {
                    if (taskRetryCount <= MAXIMUM_TASK_RETRIES) {
                        mOriginalTask.retryCount = taskRetryCount + 1;
                        mOriginalTask.lastAttempt = Calendar.getInstance();

                        // Exponentially bump up the next retry interval
                        Calendar nextAttemptCal = Calendar.getInstance();
                        nextAttemptCal.add(Calendar.MINUTE, (mOriginalTask.retryCount * 2));
                        mOriginalTask.nextAttempt = nextAttemptCal;

                        mOriginalTask.processState = ProcessState.STATE_WAITING_RETRY;
                        for (Recipient recipientToRetry : message.recipients) {
                            recipientToRetry.state = ProcessState.STATE_WAITING_RETRY;
                            mEbeanServer.update(recipientToRetry);
                        }

                    } else {
                        // If the maximum retry count has eben reached, complete the task, and fail the remaining recipients.
                        mOriginalTask.processState = ProcessState.STATE_COMPLETE;

                        for (Recipient recipientToRetry : message.recipients) {
                            recipientToRetry.state = ProcessState.STATE_FAILED;
                            recipientToRetry.failure = new RecipientFailure("MaxRetryReached");
                            mEbeanServer.update(recipientToRetry);
                        }
                    }
                }
            }

            updateTask(mOriginalTask);
        }

        @Override
        public void messageFailed(@Nonnull Message message, HardFailCause failCause) {
            mLog.e(TAG, String.format("Failed. %s response from Google for message %d",
                    failCause.name(), message.messageId));

            mOriginalTask.processState = ProcessState.STATE_FAILED;
            for (Recipient recipient : message.recipients) {
                recipient.state = ProcessState.STATE_FAILED;
            }

            mEbeanServer.save(message);
        }
    }

    private class QueueConsumer implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    mLog.d(TAG, "Consumer is waiting to take element...");
                    Task taskToProcess = mPendingTaskQueue.take();

                    mLog.d(TAG, "Consumer received Element: " + taskToProcess.taskId);
                    if (isTaskIncomplete(taskToProcess)) {

                        // Set the timestamp for now.
                        taskToProcess.lastAttempt = Calendar.getInstance();
                        updateTask(taskToProcess);

                        // Create a single response class for each separate message in the task.
                        for (Message message : taskToProcess.messages) {

                            // Set the task as processing
                            taskToProcess.processState = ProcessState.STATE_PROCESSING;

                            // Set the message recipients as processing.
                            for (Recipient recipient : message.recipients) {
                                recipient.state = ProcessState.STATE_PROCESSING;
                            }
                            updateTask(taskToProcess);

                            // Dispatch the message.
                            mGcmDispatcher.dispatchMessageAsync(message, new DispatchResponseListener(taskToProcess));
                        }
                    }

                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
