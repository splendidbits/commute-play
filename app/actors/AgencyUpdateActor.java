package actors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import controllers.SeptaAlertsController;
import models.app.AgencyUpdateType;

import javax.inject.Inject;

/**
 * Agency Update Broadcast bean which notifies all agency controller
 * providers to update.
 */
public class AgencyUpdateActor extends UntypedActor {
    public static Props props = Props.create(AgencyUpdateActor.class);

    @Inject
    private SeptaAlertsController mSeptaAlertsController;

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg != null && msg instanceof AgencyUpdateProtocol) {
            AgencyUpdateProtocol message = (AgencyUpdateProtocol) msg;

            if (message.getAgencyType().equals(AgencyUpdateType.TYPE_ALL)) {
                mSeptaAlertsController.updateAgency();

            } else if (message.getAgencyType().equals(AgencyUpdateType.TYPE_SEPTA)){
                mSeptaAlertsController.updateAgency();
            }
        }
    }
}
