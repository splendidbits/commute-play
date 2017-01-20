package injection.modules;

import com.google.inject.AbstractModule;
import injection.providers.ApplicationLifecycleProvider;
import main.ApplicationLifecycleListener;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 02/01/2017 Splendid Bits.
 */
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ApplicationLifecycleListener.class)
                .toProvider(ApplicationLifecycleProvider.class)
                .asEagerSingleton();
    }
}
