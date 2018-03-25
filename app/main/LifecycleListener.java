package main;

import annotations.CommuteEbeanServer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.CompletableFuture;

@Singleton
public class LifecycleListener {

    @Inject
    public LifecycleListener(@CommuteEbeanServer EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle) {
            applicationLifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
            ebeanServer.shutdown(true, false);
        }));
    }
}
