package main;

import actors.AgencyUpdateActor;
import actors.AgencyUpdateMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


/**
 * Starts scheduled tasks such as agency alert feed downloads, and periodic taskqueue
 * consumer verification. Runs on application startup.
 */
@Singleton
public class CommuteSchedulers {
    private static final String TAG = CommuteStartModule.class.getSimpleName();

    @Inject
    public CommuteSchedulers(ActorSystem actorSystem, @Named(AgencyUpdateActor.ACTOR_NAME) ActorRef agencyActor) {
        startAgencyUpdateSchedule(actorSystem, agencyActor);
    }

    /**
     * Start the Agencies updater using the Akka scheduler actor system.
     * http://doc.akka.io/docs/akka/1.2/java/untyped-actors.html
     */
    private void startAgencyUpdateSchedule(ActorSystem actorSystem, ActorRef agencyActor) {
        if (actorSystem != null) {
            actorSystem.scheduler()
                    .schedule(
                            Duration.create(Constants.AGENCY_UPDATE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                            Duration.create(Constants.AGENCY_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                            agencyActor,
                            new AgencyUpdateMessage(),
                            actorSystem.dispatcher(),
                            ActorRef.noSender());
        }
    }
}
