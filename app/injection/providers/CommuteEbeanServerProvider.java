package injection.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;
import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebean.config.dbplatform.postgres.PostgresPlatform;
import main.Constants;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import org.avaje.datasource.DataSourceConfig;

import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
public class CommuteEbeanServerProvider implements Provider<EbeanServer> {
    private final static String DATABASE_SERVER_TYPE_NAME = "postgres";
    private final static String SERVER_CONFIG_PREFIX = "db." + Constants.DATABASE_SERVER_NAME + ".";
    private Config mConfiguration;

    @Inject
    public CommuteEbeanServerProvider(Config configuration) {
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
        dataSourceConfig.setDriver(datasourceDriver);
        dataSourceConfig.setUsername(datasourceUsername);
        dataSourceConfig.setPassword(datasourcePassword);

        dataSourceConfig.setHeartbeatFreqSecs(60);
        dataSourceConfig.setHeartbeatTimeoutSeconds(30);
        dataSourceConfig.setMinConnections(3);
        dataSourceConfig.setMaxConnections(30);
        dataSourceConfig.setLeakTimeMinutes(1);
        dataSourceConfig.setMaxInactiveTimeSecs(30);
        dataSourceConfig.setWaitTimeoutMillis(1000 * 60);
        dataSourceConfig.setTrimPoolFreqSecs(60);
        dataSourceConfig.setCaptureStackTrace(true);

        // Set the isolation level so reads wait for uncommitted data.
        // http://stackoverflow.com/questions/16162357/transaction-isolation-levels-relation-with-locks-on-table
//        dataSourceConfig.setIsolationLevel(Transaction.READ_UNCOMMITTED);

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
        serverConfig.setName(Constants.DATABASE_SERVER_NAME);
        serverConfig.setDataSourceConfig(dataSourceConfig);
        serverConfig.setDatabasePlatform(new PostgresPlatform());
        serverConfig.setDatabasePlatformName(DATABASE_SERVER_TYPE_NAME);
        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(true);
        serverConfig.setClasses(models);
        serverConfig.setDdlGenerate(true);
        serverConfig.setUpdatesDeleteMissingChildren(false);
        serverConfig.setUpdateChangesOnly(true);

        return EbeanServerFactory.create(serverConfig);
    }
}