package actors;

import agency.inapp.InAppMessageUpdate;
import agency.septa.SeptaAgencyUpdate;
import akka.actor.UntypedActor;
import com.google.inject.Inject;
import enums.AgencyUpdateType;

/**
 * Agency Update Broadcast bean which notifies all agency controller
 * providers to update.
 */
public class AgencyUpdateActor extends UntypedActor {
    public static final String ACTOR_NAME = "agency-update-actor";
    private SeptaAgencyUpdate mSeptaAgencyUpdate;
    private InAppMessageUpdate mInAppUpdate;

    @Inject
    public AgencyUpdateActor(SeptaAgencyUpdate septaUpdate, InAppMessageUpdate inAppUpdate) {
        mSeptaAgencyUpdate = septaUpdate;
        mInAppUpdate = inAppUpdate;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg != null && msg instanceof AgencyUpdateProtocol) {
            AgencyUpdateMessage message = (AgencyUpdateMessage) msg;
            sender().tell("Actor fired for Agency type: " + message.getAgencyType(), self());
            AgencyUpdateType agencyType = message.getAgencyType();

            switch (agencyType) {
                case TYPE_ALL:
                    mSeptaAgencyUpdate.startAgencyUpdate();
                    mInAppUpdate.startAgencyUpdate();
                    break;

                case TYPE_IN_APP:
                    mInAppUpdate.startAgencyUpdate();
                    break;

                case TYPE_SEPTA:
                    mSeptaAgencyUpdate.startAgencyUpdate();
                    break;
            }
        }
    }
}
