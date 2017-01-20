package injection.providers;

import com.avaje.ebean.EbeanServer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import main.ApplicationLifecycleListener;
import play.inject.ApplicationLifecycle;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 03/01/2017 Splendid Bits.
 */
public class ApplicationLifecycleProvider implements Provider<ApplicationLifecycleListener> {
    private final ApplicationLifecycle mApplicationLifecycle;
    private final EbeanServer mEbeanServer;

    @Inject
    public ApplicationLifecycleProvider(EbeanServer ebeanServer, ApplicationLifecycle applicationLifecycle) {
        mEbeanServer = ebeanServer;
        mApplicationLifecycle = applicationLifecycle;
    }

    @Override
    public ApplicationLifecycleListener get() {
        return new ApplicationLifecycleListener(mEbeanServer, mApplicationLifecycle);
    }
}
