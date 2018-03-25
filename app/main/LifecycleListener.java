package main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.CompletableFuture;

@Singleton
public class LifecycleListener {

    @Inject
    public LifecycleListener(ApplicationLifecycle applicationLifecycle) {
            applicationLifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
                EbeanServer ebeanServer = Ebean.getServer(Constants.DATABASE_SERVER_NAME);
                if (ebeanServer != null) {
                    ebeanServer.shutdown(true, false);
                }
        }));
    }
}
