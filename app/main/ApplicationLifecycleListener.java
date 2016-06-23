package main;

import com.avaje.ebean.EbeanServer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 6/14/16 Splendid Bits.
 */
@Singleton
public class ApplicationLifecycleListener {
    private EbeanServer mEbeanServer;

    @Inject
    public ApplicationLifecycleListener(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle) {
        mEbeanServer = ebeanServer;
        applicationLifecycle.addStopHook(new CleanupDatabase());
    }

    /*
     * CompletableFuture for when the application lifecycle has initiated the shutdown.
     */
    private class CleanupDatabase implements Callable<CompletionStage<?>> {
        @Override
        public CompletionStage<?> call() throws Exception {
            mEbeanServer.shutdown(true, true);

            return CompletableFuture.completedFuture(true);
        }
    }
}
