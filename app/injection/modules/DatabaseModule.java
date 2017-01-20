package injection.modules;

import com.avaje.ebean.EbeanServer;
import com.google.inject.AbstractModule;
import injection.providers.CommuteEbeanServerProvider;

/**
 * GuiseModule for invoking the ebean Database provider.
 */
public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EbeanServer.class)
                .toProvider(CommuteEbeanServerProvider.class)
                .asEagerSingleton();
    }
}