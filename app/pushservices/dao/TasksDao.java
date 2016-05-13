package pushservices.dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import pushservices.enums.RecipientState;
import pushservices.models.database.Task;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Task Message and Queue persistence methods.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/10/16 Splendid Bits.
 */
public class TasksDao {

    @Inject
    private EbeanServer mEbeanServer;

    @Inject
    public TasksDao() {
    }

    /**
     * Inserts a task in the TaskQueue datastore, and cascades through
     * children.
     *
     * @param task the task to update.
     * @return true if the task was updated.
     */
    public boolean insertTask(@Nonnull Task task) {
        try {
            if (fetchTask(task.id) == null) {
                mEbeanServer.insert(task);
            }

        } catch (Exception e) {
            Logger.error("Error inserting new task", e);
            return false;
        }

        // Return true if the task has an id (and thus was inserted)
        return task.id != null;
    }

    /**
     * Inserts a task in the TaskQueue datastore, and cascades through
     * children.
     *
     * @param task the task to update.
     * @return true if the task was updated.
     */
    public boolean updateTask(@Nonnull Task task) {
        try {
            if (fetchTask(task.id) != null) {
                mEbeanServer.update(task);
            }

        } catch (Exception e) {
            Logger.error("Error updating task", e);
            return false;
        }

        // Return true if the task has an id (and thus was updated)
        return task.id != null;
    }

    /**
     * Get all {@link Task}s from the database which have not yet fully
     * completed.
     */
    @Nonnull
    public List<Task> fetchPendingTasks() {
        List<Task> pendingTasks = new ArrayList<>();

        try {
            OrderBy<Task> priorityOrder = new OrderBy<>("priority");
            priorityOrder.reverse();

            List<Task> tasks = mEbeanServer.find(Task.class)
                    .setOrderBy(priorityOrder)
                    .fetch("messages.task")
                    .fetch("messages.credentials")
                    .where()
                    .disjunction()
                    .eq("messages.recipients.state", "")
                    .ne("messages.recipients.state", RecipientState.STATE_COMPLETE)
                    .endJunction()
                    .filterMany("messages").ne("recipients.state", RecipientState.STATE_COMPLETE)
                    .filterMany("messages").ne("recipients.state", RecipientState.STATE_FAILED)
                    .findList();

            // Add the saved pending tasks back into the queue
            if (tasks != null) {
                pendingTasks.addAll(tasks);
            }
        } catch (Exception e) {
            Logger.error("Error fetching message tasks.", e);
        }
        return pendingTasks;
    }

    /**
     * Fetch a stored task for a specific ebean id.
     *
     * @param taskId the id of the task.
     * @return Null, or a task if found.
     */
    @Nullable
    public Task fetchTask(Long taskId) {
        if (taskId != null) {
            try {
                return mEbeanServer.find(Task.class)
                        .where()
                        .eq("id", taskId)
                        .findUnique();

            } catch (Exception e) {
                Logger.error(String.format("Error getting task for id %d.", taskId), e);
            }
        }
        return null;
    }

}
