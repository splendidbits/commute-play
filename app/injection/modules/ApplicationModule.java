package injection.modules;

import actors.AgencyUpdateActor;
import com.google.inject.AbstractModule;
import main.LifecycleListener;
import play.libs.akka.AkkaGuiceSupport;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 02/01/2017 Splendid Bits.
 */
public class ApplicationModule extends AbstractModule implements AkkaGuiceSupport {

    @Override
    protected void configure() {
        bindActor(AgencyUpdateActor.class, AgencyUpdateActor.ACTOR_NAME);

        bind(LifecycleListener.class)
                .asEagerSingleton();
    }
}
