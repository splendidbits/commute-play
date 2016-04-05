package main;

import actors.AgencyUpdateActor;
import com.avaje.ebean.EbeanServer;
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;
import services.TaskQueue;

/**
 * Runs on application startup.
 */
public class CommuteServerStartModule extends AbstractModule implements AkkaGuiceSupport {

    @Override
    protected void configure() {
        // Bind the database server
        bind(EbeanServer.class)
                .toProvider(CommuteEbeanServerProvider.class)
                .asEagerSingleton();

        bindActor(AgencyUpdateActor.class, AgencyUpdateActor.ACTOR_NAME);

        // Bind the scheduling system.
        bind(CommuteSchedulers.class).asEagerSingleton();

        // Bind the TaskQueue eagerly.
        bind(TaskQueue.class).asEagerSingleton();
    }
}
