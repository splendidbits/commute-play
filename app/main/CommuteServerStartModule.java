package main;

import com.avaje.ebean.EbeanServer;
import com.google.inject.AbstractModule;
import play.libs.akka.AkkaGuiceSupport;

/**
 * Runs on application startup.
 */
public class CommuteServerStartModule extends AbstractModule implements AkkaGuiceSupport {
    @Override
    protected void configure() {
        bind(EbeanServer.class)
                .toProvider(CommuteEbeanServerProvider.class)
                .asEagerSingleton();

        bind(StartSchedulers.class).asEagerSingleton();
    }
}
