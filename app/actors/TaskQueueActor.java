package actors;

import akka.actor.UntypedActor;

import javax.inject.Inject;

/**
 * TaskQueue actor which invokes the taskqueue.
 */
public class TaskQueueActor extends UntypedActor {
    @Inject
    public TaskQueueActor() {
    }

    @Override
    public void onReceive(Object message) throws Exception {

    }
}