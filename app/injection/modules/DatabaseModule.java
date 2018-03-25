package injection.modules;

import annotations.CommuteEbeanServer;
import com.google.inject.AbstractModule;
import injection.providers.CommuteEbeanServerProvider;
import io.ebean.EbeanServer;

/**
 * GuiseModule for invoking the ebean Database provider.
 */
public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EbeanServer.class)
                .annotatedWith(CommuteEbeanServer.class)
                .toProvider(CommuteEbeanServerProvider.class)
                .asEagerSingleton();
    }
}