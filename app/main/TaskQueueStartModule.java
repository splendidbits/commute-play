package main;

import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

/**
 * Runs on application startup.
 */
public class TaskQueueStartModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        bind(InvokeSchedulers.class).asEagerSingleton();
    }
}
