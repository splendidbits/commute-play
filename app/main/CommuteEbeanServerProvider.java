package main;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import play.Configuration;

import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
public class CommuteEbeanServerProvider implements Provider<EbeanServer> {
    private final static String SERVER_NAME = "commute_gcm_server";
    private final static String SERVER_CONFIG_PREFIX = "db." + SERVER_NAME + ".";

    private Configuration mConfiguration;

    @Inject
    public CommuteEbeanServerProvider(Configuration configuration) {
        mConfiguration = configuration;
    }

    @Override
    public EbeanServer get() {
        String datasourceUrl = mConfiguration.getString(SERVER_CONFIG_PREFIX + "url");
        String datasourceUsername = mConfiguration.getString(SERVER_CONFIG_PREFIX + "username");
        String datasourcePassword = mConfiguration.getString(SERVER_CONFIG_PREFIX + "password");
        String datasourceDriver = mConfiguration.getString(SERVER_CONFIG_PREFIX + "driver");

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUrl(datasourceUrl);
        dataSourceConfig.setMaxConnections(1024);
        dataSourceConfig.setDriver(datasourceDriver);
        dataSourceConfig.setUsername(datasourceUsername);
        dataSourceConfig.setPassword(datasourcePassword);
        dataSourceConfig.setHeartbeatTimeoutSeconds(60);
        dataSourceConfig.setCaptureStackTrace(true);

        // Agency, API Account, Device, and TaskQueue models.
        ArrayList<Class<?>> models = new ArrayList<>();
        models.add(Account.class);
        models.add(PlatformAccount.class);
        models.add(Agency.class);
        models.add(Route.class);
        models.add(Alert.class);
        models.add(Location.class);
        models.add(Device.class);
        models.add(Subscription.class);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setDatabasePlatform(new com.avaje.ebean.config.dbplatform.PostgresPlatform());
        serverConfig.setName(SERVER_NAME);
        serverConfig.setDefaultServer(true);
        serverConfig.setUpdatesDeleteMissingChildren(true);
        serverConfig.setRegister(true);
        serverConfig.setClasses(models);
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setUpdatesDeleteMissingChildren(true);

        return EbeanServerFactory.create(serverConfig);
    }
}