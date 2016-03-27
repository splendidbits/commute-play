package main;

import actors.AgencyUpdateActor;
import actors.TaskQueueActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import controllers.AgencyController;
import scala.concurrent.duration.Duration;
import services.TaskQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;


/**
 * Runs on application startup.
 */
@Singleton
public class InvokeSchedulers {

    private static final int TASK_QUEUE_INITIAL_DELAY_SECONDS = 60;
    private static final int TASK_QUEUE_INTERVAL_SECONDS = 15;

    private static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 60;
    private static final int AGENCY_UPDATE_INTERVAL_SECONDS = 60;

    public static final String AGENCY_RECEIVER_ACTOR_NAME = "agency-updater";
    public static final String AGENCY_UPDATE_ACTOR_NAME = "agency-update-actor";
    public static final String TASK_QUEUE_ACTOR_NAME = "task-queue-actor";


    private static final String TAG = TaskQueueStartModule.class.getSimpleName();
    private final ActorRef mAgencyControllerActor;
    private ActorSystem mActorSystem = null;
    private ActorRef mAgencyUpdateActor = null;
    private ActorRef mTaskQueueActor = null;

    private TaskQueue mTaskQueue = null;

    @Inject
    public InvokeSchedulers(ActorSystem system) {
        mActorSystem = system;
        mAgencyUpdateActor = system.actorOf(Props.create(AgencyUpdateActor.class), AGENCY_UPDATE_ACTOR_NAME);
        mAgencyControllerActor = system.actorOf(Props.create(AgencyController.class), AGENCY_RECEIVER_ACTOR_NAME);
        mTaskQueueActor = system.actorOf(Props.create(TaskQueueActor.class), TASK_QUEUE_ACTOR_NAME);

        startAgencyUpdateSchedule();
        startTaskQueueSchedule();
    }

    /**
     * Start the TaskQueue system scheduler.
     * TODO: Convert to Akka Actor Scheduler.
     */
    private void startTaskQueueSchedule() {
        mTaskQueue = TaskQueue.getInstance();

        mActorSystem.scheduler().schedule(
                Duration.create(TASK_QUEUE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                Duration.create(TASK_QUEUE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Invoking TaskQueue Service" + System.currentTimeMillis());
                        mTaskQueue.sweep();
                    }
                },
                mActorSystem.dispatcher()
        );
    }

    /**
     * Start the Agencies updater using the actor subsystem.
     */
    private void startAgencyUpdateSchedule() {
        if (mActorSystem != null && mTaskQueueActor != null) {
            Log.d(TAG, "Starting agency update actor schedule.");

            mActorSystem.scheduler()
                    .schedule(Duration.create(AGENCY_UPDATE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                            Duration.create(AGENCY_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                            mAgencyControllerActor,
                            mTaskQueueActor,
                            mActorSystem.dispatcher(),
                            ActorRef.noSender());
        } else {
            Log.e(TAG, "Error starting agency update actor schedule.");
        }
    }
}
