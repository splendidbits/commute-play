package main;

import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;
import services.pushservices.TaskQueue;

import java.util.concurrent.CompletableFuture;

@Singleton
public class LifecycleListener {
    public LifecycleListener(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle, TaskQueue taskQueue) {
        taskQueue.startup();

        applicationLifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
            ebeanServer.shutdown(true, false);
        }));
    }
}
