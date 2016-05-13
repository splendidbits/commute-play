package pushservices.services;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import pushservices.dao.TasksDao;

/**
 * Provides a single thread-safe instance of {@link TaskQueue} for dependency injection.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/12/16 Splendid Bits.
 */
public class TaskQueueProvider implements Provider<TaskQueue> {

    @Inject
    private TasksDao mTasksDao;

    @Inject
    private GoogleMessageDispatcher mGoogleMessageDispatcher;

    @Provides
    @Override
    public TaskQueue get() {
        return new TaskQueue(mTasksDao, mGoogleMessageDispatcher);
    }
}
