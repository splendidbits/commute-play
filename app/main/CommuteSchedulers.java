package main;

import actors.AgencyUpdateActor;
import actors.AllAgencyUpdateMessage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import scala.concurrent.duration.Duration;
import services.TaskQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Runs on application startup.
 */
@Singleton
public class CommuteSchedulers {
    private static final String TAG = CommuteServerStartModule.class.getSimpleName();

    private static final int TASK_QUEUE_INITIAL_DELAY_SECONDS = 60;
    private static final int TASK_QUEUE_INTERVAL_SECONDS = 15;
    private static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 60;
    private static final int AGENCY_UPDATE_INTERVAL_SECONDS = 60;
    private static final String ACTOR_SYSTEM_NAME = "commute_actor_system";

    private ActorRef mAgencyUpdateActor;
    private ActorSystem mActorSystem;

    @Inject
    private Log mLog;

    @Inject
    private TaskQueue mTaskQueue;

    @Inject
    public CommuteSchedulers(Log log, TaskQueue taskQueue) {
        mLog = log;
        mTaskQueue = taskQueue;

        startSchedulers();
    }

    /**
     * Dispatch the periodic schedulers.
     */
    private void startSchedulers() {
        mActorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME);
        mAgencyUpdateActor = mActorSystem.actorOf(AgencyUpdateActor.props);

        startAgencyUpdateSchedule();
        startTaskQueueSchedule();
    }

    /**
     * Start the TaskQueue system scheduler.
     */
    private void startTaskQueueSchedule() {
        mActorSystem.scheduler().schedule(
                Duration.create(TASK_QUEUE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                Duration.create(TASK_QUEUE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                new Runnable() {
                    @Override
                    public void run() {
                        mLog.d(TAG, "Sweeping TaskQueue Service" + System.currentTimeMillis());
                        mTaskQueue.sweep();
                    }
                },
                mActorSystem.dispatcher()
        );
    }

    /**
     * Start the Agencies updater using the Akka scheduler
     * actor subsystem. http://doc.akka.io/docs/akka/1.2/java/untyped-actors.html
     */
    private void startAgencyUpdateSchedule() {
        if (mActorSystem != null) {
            mActorSystem.scheduler()
                    .schedule(Duration.create(AGENCY_UPDATE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                            Duration.create(AGENCY_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                            mAgencyUpdateActor,
                            new AllAgencyUpdateMessage(),
                            mActorSystem.dispatcher(),
                            ActorRef.noSender());

        } else {
            mLog.e(TAG, "Error starting agency update actor schedule.");
        }
    }
}
