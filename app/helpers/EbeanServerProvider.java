package helpers;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.Provider;

import javax.inject.Inject;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
public class EbeanServerProvider implements Provider<EbeanServer> {

    @Inject
    public EbeanServerProvider() {
    }

    @Override
    public EbeanServer get() {
        ServerConfig config = new ServerConfig();
        config.setName("commute_gcm_server");

        // load configuration from ebean.properties
        config.loadFromProperties();
        config.setDefaultServer(true);

        return EbeanServerFactory.create(config);
    }
}
