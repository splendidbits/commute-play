package main;

import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * CompletableFuture for when the application lifecycle has initiated the shutdown.
 */
@Singleton
public class ApplicationLifecycleListener implements Callable<CompletionStage<Boolean>> {
    private EbeanServer mEbeanServer;

    public ApplicationLifecycleListener(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle) {
        mEbeanServer = ebeanServer;
        applicationLifecycle.addStopHook(this);
    }

    @Override
    public CompletionStage<Boolean> call() throws Exception {
        mEbeanServer.shutdown(true, true);
        return CompletableFuture.completedFuture(true);
    }
}