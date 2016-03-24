package main;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import controllers.AgencyController;
import actors.AgencyUpdateActor;
import play.Application;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import services.TaskQueue;

import java.util.concurrent.TimeUnit;

/**
 * Runs on application startup.
 */
public class ApplicationStart extends play.GlobalSettings {
    private static final int TASK_QUEUE_INITIAL_DELAY_SECONDS = 60;
    private static final int TASK_QUEUE_INTERVAL_SECONDS = 15;

    private static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 60;
    private static final int AGENCY_UPDATE_INTERVAL_SECONDS = 60;

    private static final String TAG = ApplicationStart.class.getSimpleName();
    private static TaskQueue mTaskQueue = null;

    private static final String AGENCY_UPDATE_ACTOR_NAME = "agency-update-actor";
    private static final String ACTOR_SYSTEM_NAME = "actor-system";
    private static ActorSystem mActorSystem;

    @Override
    public void onStart(Application app) {
        super.onStart(app);
        mActorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME);

        invokeTaskQueueSchedule();
        invokeAgencyUpdateSchedule();
    }

    @Override
    public void onStop(Application app) {
        super.onStop(app);
        mTaskQueue.shutdown();
        mActorSystem.shutdown();
    }

    /**
     * Start the TaskQueue system scheduler.
     * TODO: Convert to Akka Actor Scheduler.
     */
    private void invokeTaskQueueSchedule() {
        mTaskQueue = TaskQueue.getInstance();

        Akka.system().scheduler().schedule(
                Duration.create(TASK_QUEUE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                Duration.create(TASK_QUEUE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Invoking TaskQueue Service" + System.currentTimeMillis());
                        mTaskQueue.sweep();
                    }
                },
                Akka.system().dispatcher()
        );
    }

    /**
     * Start the Agencies updater using the actor subsystem.
     */
    private void invokeAgencyUpdateSchedule() {
        final ActorRef messageActor = mActorSystem.actorOf(
                Props.create(AgencyUpdateActor.class), AGENCY_UPDATE_ACTOR_NAME);

        final ActorRef receiveActor = mActorSystem.actorOf(
                Props.create(AgencyController.AgencyAkkaActor.class), AGENCY_UPDATE_ACTOR_NAME);

        if (mActorSystem != null && receiveActor != null) {
            Log.d(TAG, "Starting agency update actor schedule.");

            Akka.system().scheduler()
                    .schedule(Duration.create(AGENCY_UPDATE_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS),
                            Duration.create(AGENCY_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
                            receiveActor,
                            messageActor,
                            mActorSystem.dispatcher(),
                            ActorRef.noSender());
        } else {
            Log.e(TAG, "Error starting agency update actor schedule.");
        }
    }
}
