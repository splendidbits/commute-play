package actors;

import agency.SeptaAgencyUpdate;
import akka.actor.UntypedActor;
import com.google.inject.Inject;

/**
 * Agency Update Broadcast bean which notifies all agency controller
 * providers to update.
 */
public class AgencyUpdateActor extends UntypedActor {
    public static final String ACTOR_NAME = "agency-update-actor";

    @Inject
    private SeptaAgencyUpdate mSeptaAgencyUpdate;

    @Inject
    public AgencyUpdateActor(SeptaAgencyUpdate septaAgencyUpdate) {
        mSeptaAgencyUpdate = septaAgencyUpdate;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg != null && msg instanceof AgencyUpdateProtocol) {
            AgencyUpdateProtocol agencyUpdateMessage = (AgencyUpdateProtocol) msg;
            sender().tell("Actor fired for Agency type: " + agencyUpdateMessage.getAgencyType(), self());

            mSeptaAgencyUpdate.startAgencyUpdate();
        }
    }
}
