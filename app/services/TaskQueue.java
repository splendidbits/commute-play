package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import interfaces.IPushResponse;
import main.Constants;
import main.Log;
import models.app.MessageResult;
import models.taskqueue.Task;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The singleton class that handles all GCM task jobs.
 */
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private EbeanServer mEbeanServer;
    private List<Task> mPendingTasks = Collections.synchronizedList(new ArrayList<>());

    /**
     * Get a THREADSAFE Syncronised instance of the TaskQueue
     *
     * @return An instance of the TaskQueue which contain locks.
     */
    private static class Loader {
        static final TaskQueue INSTANCE = new TaskQueue();
    }

    public static TaskQueue getInstance() {
        return Loader.INSTANCE;
    }

    private TaskQueue() {
        try {
            mEbeanServer = Ebean.getServer(Constants.COMMUTE_GCM_DB_SERVER);
            if (mEbeanServer != null) {
                clearPendingTasks();
            }
        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
        }
    }

    /**
     * Set each task that is currently in rotation to not be.
     */
    private void clearPendingTasks() {
        List<Task> tasks = mEbeanServer.find(Task.class)
                .where()
                .eq("inProcess", true)
                .findList();

        // Save
        if (tasks != null) {
            for (Task task : tasks) {
                task.inProcess = false;
                task.save();
            }
        }
    }

    /**
     * Run through all the jobs that TaskQueue needs to perform. Should be fired
     * externally through a system cronjob or a platform services such as Akka.
     */
    public void sweep(){
        Log.d(TAG, "Performing a sweep of TaskQueue.");
    }

    /**
     * Add a new task to the TaskQueue.
     * @param task
     */
    public void addTask(@Nonnull Task task) {
        mPendingTasks.add(task);
    }

    /**
     * Call before shutting down application server.
     */
    public void shutdown(){
        clearPendingTasks();
    }

    /**
     * Results from individual tasks
     */
    private class GcmResponse implements IPushResponse{

        @Override
        public void messageSuccess(MessageResult result) {

        }

        @Override
        public void messageFailed(MessageResult result) {

        }
    }
}
