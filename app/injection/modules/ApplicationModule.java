package injection.modules;

import com.google.inject.AbstractModule;
import main.LifecycleListener;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 02/01/2017 Splendid Bits.
 */
public class ApplicationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LifecycleListener.class)
                .asEagerSingleton();
    }
}
