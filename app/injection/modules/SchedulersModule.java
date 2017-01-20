package injection.modules;

import actors.AgencyUpdateActor;
import com.google.inject.AbstractModule;
import main.CommuteSchedulers;
import play.libs.akka.AkkaGuiceSupport;

/**
 * Guise Module for invoking Agency schedule updates.
 */
public class SchedulersModule extends AbstractModule implements AkkaGuiceSupport {

    @Override
    protected void configure() {
        bindActor(AgencyUpdateActor.class, AgencyUpdateActor.ACTOR_NAME);
        bind(CommuteSchedulers.class).asEagerSingleton();
    }
}