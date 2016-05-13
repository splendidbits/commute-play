package main;

import actors.AgencyUpdateActor;
import com.avaje.ebean.EbeanServer;
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;
import pushservices.main.TaskQueueScheduler;
import pushservices.services.TaskQueue;
import pushservices.services.TaskQueueProvider;

/**
 * Runs on application startup.
 */
public class CommuteStartModule extends AbstractModule implements AkkaGuiceSupport {

    @Override
    protected void configure() {
        // Bind the database server
        bind(EbeanServer.class)
                .toProvider(CommuteEbeanServerProvider.class)
                .asEagerSingleton();

        // Bind the scheduling system.
        bind(CommuteSchedulers.class).asEagerSingleton();

        bindActor(AgencyUpdateActor.class, AgencyUpdateActor.ACTOR_NAME);

        // TODO: PULL INTO PUSH-SERVICES
        // Bind TaskQueue as an eager singleton. An eager singleton is a sad thing to be.
        bind(TaskQueue.class)
                .toProvider(TaskQueueProvider.class)
                .asEagerSingleton();

        // Bind the scheduling system.
        bind(TaskQueueScheduler.class).asEagerSingleton();
        // TODO: END PULL INTO PUSH-SERVICES
    }
}
