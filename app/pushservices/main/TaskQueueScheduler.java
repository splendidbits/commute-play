package pushservices.main;

import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import main.Constants;
import pushservices.services.TaskQueue;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


/**
 * Starts scheduled tasks such as agency alert feed downloads, and periodic taskqueue
 * consumer verification. Runs on application startup.
 */
@Singleton
public class TaskQueueScheduler {

    @Inject
    private TaskQueue mTaskQueue;

    @Inject
    private ActorSystem mActorSystem;

    @Inject
    public TaskQueueScheduler(ActorSystem actorSystem) {
        mActorSystem = actorSystem;
        verifyTaskQueueConsumerRunning();
    }

    /**
     * Periodically verify that the TaskQueue Queue consumer is up and running,
     * if not, restart it.
     */
    private void verifyTaskQueueConsumerRunning() {
        mActorSystem.scheduler().schedule(Duration.create(0, TimeUnit.SECONDS),
                Duration.create(Constants.TASKQUEUE_CHECK_CONSUMER_RUNNING_MINS, TimeUnit.MINUTES),
                new Runnable() {
                    @Override
                    public void run() {
                        mTaskQueue.start();
                    }
                },

                mActorSystem.dispatcher()
        );
    }
}
