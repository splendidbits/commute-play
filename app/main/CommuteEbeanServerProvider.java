package main;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Transaction;
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
import org.avaje.datasource.DataSourceConfig;
import play.Configuration;

import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
public class CommuteEbeanServerProvider implements Provider<EbeanServer> {
    private final static String DATABASE_SERVER_TYPE_NAME = "postgres";
    private final static String SERVER_CONFIG_PREFIX = "db." + Constants.DATABASE_SERVER_NAME + ".";
    private final static int DATABASE_HEARTBEAT_SECS = 30;

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
        dataSourceConfig.setAutoCommit(false);
        dataSourceConfig.setHeartbeatFreqSecs(DATABASE_HEARTBEAT_SECS);
        dataSourceConfig.setHeartbeatTimeoutSeconds(120);
        dataSourceConfig.setMaxConnections(100);
        dataSourceConfig.setUrl(datasourceUrl);
        dataSourceConfig.setDriver(datasourceDriver);
        dataSourceConfig.setUsername(datasourceUsername);
        dataSourceConfig.setPassword(datasourcePassword);
        dataSourceConfig.setCaptureStackTrace(true);

        // Set the isolation level so reads wait for uncommitted data.
        // http://stackoverflow.com/questions/16162357/transaction-isolation-levels-relation-with-locks-on-table
        dataSourceConfig.setIsolationLevel(Transaction.REPEATABLE_READ);

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
        serverConfig.setDatabaseSequenceBatchSize(1);
        serverConfig.setName(Constants.DATABASE_SERVER_NAME);
        serverConfig.setDatabasePlatform(new com.avaje.ebean.config.dbplatform.PostgresPlatform());
        serverConfig.setDatabasePlatformName(DATABASE_SERVER_TYPE_NAME);
        serverConfig.setDefaultServer(true);
        serverConfig.setUpdatesDeleteMissingChildren(true);
        serverConfig.setUpdateAllPropertiesInBatch(true);
        serverConfig.setRegister(true);
        serverConfig.setAutoCommitMode(false);
        serverConfig.setClasses(models);
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setDdlGenerate(true);

        return EbeanServerFactory.create(serverConfig);
    }
}