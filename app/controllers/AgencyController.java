package controllers;

import akka.actor.UntypedActor;
import actors.AgencyUpdateActor;
import play.mvc.Controller;

public abstract class AgencyController extends Controller{
    protected static final int AGENCY_DOWNLOAD_TIMEOUT_MS = 1000 * 60;

    public abstract void updateAgency();

    /**
     * Receives messages from the Akka broadcast system.
     */
    public class AgencyAkkaActor extends UntypedActor {

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof AgencyUpdateActor) {
                updateAgency();
            }
        }
    }
}
