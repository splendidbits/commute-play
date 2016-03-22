package actors;

import akka.actor.UntypedActor;

/**
 * Agency Update Broadcast bean which notifies all agency controller
 * providers to update.
 */
public class AgencyUpdateActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {

    }
}
