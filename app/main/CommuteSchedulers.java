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

import java.util.concurrent.TimeUnit;


/**
 * Runs on application startup.
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
    }

    /**
     * Start the TaskQueue system scheduler.
     *
     * @deprecated in favour of the TaskQueue running it's own polling thread.
     */
    private void startTaskQueueSchedule(ActorSystem actorSystem) {
        actorSystem.scheduler().schedule(
                Duration.create(Constants.TASK_QUEUE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                Duration.create(Constants.TASK_QUEUE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                new Runnable() {
                    @Override
                    public void run() {
                        mLog.d(TAG, "Sweeping TaskQueue Service" + System.currentTimeMillis());
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
