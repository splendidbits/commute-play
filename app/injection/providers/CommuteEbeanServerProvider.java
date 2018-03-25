package injection.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import main.Constants;
import play.db.ebean.EbeanConfig;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
public class CommuteEbeanServerProvider implements Provider<EbeanServer> {
    private final ServerConfig serverConfig;

    @Inject
    public CommuteEbeanServerProvider(EbeanConfig ebeanConfig) {
        serverConfig = ebeanConfig.serverConfigs().get(Constants.DATABASE_SERVER_NAME);
        Ebean.register(EbeanServerFactory.create(serverConfig), true);
    }

    @Override
    public EbeanServer get() {
        return EbeanServerFactory.create(serverConfig);
    }
}