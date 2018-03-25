package injection.modules;

import com.google.inject.AbstractModule;
import injection.providers.CommuteEbeanConfigProvider;
import play.db.ebean.EbeanConfig;

/**
 * GuiseModule for invoking the ebean Database provider.
 */
public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EbeanConfig.class)
                .toProvider(CommuteEbeanConfigProvider.class)
                .asEagerSingleton();
    }
}