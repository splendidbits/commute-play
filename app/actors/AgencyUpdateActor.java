package actors;

import akka.actor.UntypedActor;

import javax.inject.Inject;

/**
 * Agency Update Broadcast bean which notifies all agency controller
 * providers to update.
 */
public class AgencyUpdateActor extends UntypedActor {
    @Inject
    public AgencyUpdateActor() {
    }

    @Override
    public void onReceive(Object message) throws Exception {

    }
}
