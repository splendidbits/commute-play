package injection.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import main.LifecycleListener;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 03/01/2017 Splendid Bits.
 */
@Singleton
public class LifecycleListenerProvider implements Provider<LifecycleListener> {
    private final LifecycleListener mLifecycleListener;

    @Inject
    public LifecycleListenerProvider(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        mLifecycleListener = new LifecycleListener(ebeanServer, applicationLifecycle, taskQueue);
    }

    @Override
    public LifecycleListener get() {
        return mLifecycleListener;
    }
}
