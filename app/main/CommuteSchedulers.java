package main;

import actors.AgencyUpdateActor;
import actors.AgencyUpdateMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import pushservices.services.TaskQueue;
import scala.concurrent.duration.Duration;
import services.splendidlog.Log;

import java.util.concurrent.TimeUnit;


/**
 * Starts scheduled tasks such as agency alert feed downloads, and periodic taskqueue
 * consumer verification. Runs on application startup.
 */
@Singleton
public class CommuteSchedulers {
    private static final String TAG = CommuteServerStartModule.class.getSimpleName();

    @Inject
    private Log mLog;

    @Inject
    private TaskQueue mTaskQueue;


    @Inject
    public CommuteSchedulers(Log log, TaskQueue taskQueue, ActorSystem actorSystem,
                             @Named(AgencyUpdateActor.ACTOR_NAME) ActorRef agencyActor) {
        mLog = log;
        mTaskQueue = taskQueue;

        startAgencyUpdateSchedule(actorSystem, agencyActor);
        verifyTaskQueueConsumerRunning(actorSystem);
    }

    /**
     * Periodically verify that the TaskQueue Queue consumer is up and running,
     * if not, restart it.
     */
    private void verifyTaskQueueConsumerRunning(ActorSystem actorSystem) {
        actorSystem.scheduler().schedule(Duration.create(0, TimeUnit.SECONDS),
                Duration.create(Constants.TASKQUEUE_CHECK_CONSUMER_RUNNING_MINS, TimeUnit.MINUTES),
                new Runnable() {
                    @Override
                    public void run() {
                        mTaskQueue.start();
                    }
                },
                actorSystem.dispatcher()
        );
    }

    /**
     * Start the Agencies updater using the Akka scheduler
     * actor subsystem. http://doc.akka.io/docs/akka/1.2/java/untyped-actors.html
     */
    private void startAgencyUpdateSchedule(ActorSystem actorSystem, ActorRef agencyActor) {
        if (actorSystem != null) {
            actorSystem.scheduler()
                    .schedule(Duration.create(Constants.AGENCY_UPDATE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                            Duration.create(Constants.AGENCY_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                            agencyActor,
                            new AgencyUpdateMessage(),
                            actorSystem.dispatcher(),
                            ActorRef.noSender());

        } else {
            mLog.e(TAG, "Error starting agency update actor schedule.");
        }
    }
}
