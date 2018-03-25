package main;

import actors.AgencyUpdateActor;
import actors.AgencyUpdateMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import enums.AgencyUpdateType;
import io.ebean.EbeanServer;
import play.inject.ApplicationLifecycle;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Named;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class LifecycleListener {

    @Inject
    public LifecycleListener(ApplicationLifecycle lifecycle, ActorSystem actorSystem,
                             @Named(AgencyUpdateActor.ACTOR_NAME) ActorRef actor, EbeanServer ebeanServer) {

        FiniteDuration initialDelay = Duration.create(10, TimeUnit.SECONDS);
        FiniteDuration updateInterval = Duration.create(45, TimeUnit.SECONDS);

        // Start the Agency polling.
        AgencyUpdateMessage message = new AgencyUpdateMessage(AgencyUpdateType.TYPE_ALL);
        actorSystem.scheduler().schedule(
                initialDelay, updateInterval,
                actor,
                message,
                actorSystem.dispatchers().defaultGlobalDispatcher(),
                ActorRef.noSender()
        );

        lifecycle.addStopHook(() -> CompletableFuture.runAsync(() -> {
            actorSystem.eventStream().unsubscribe(actor);
            ebeanServer.shutdown(true, false);
        }));
    }
}
