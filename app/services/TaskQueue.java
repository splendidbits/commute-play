package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import main.Constants;
import main.Log;
import models.taskqueue.Task;

import javax.annotation.Nonnull;

/**
 * The singleton class that handles all GCM task jobs.
 */
public class TaskQueue {
    private static final String TAG = TaskQueue.class.getSimpleName();
    private EbeanServer mEbeanServer;

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

        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
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

    }
}
