package main;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.Provider;
import models.accounts.Account;
import models.accounts.Platform;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.registrations.Registration;
import models.registrations.Subscription;
import models.taskqueue.*;

import java.util.ArrayList;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/2/16 Splendid Bits.
 */
class CommuteEbeanServerProvider implements Provider<EbeanServer> {

    private String name = "commute_gcm_server";
    private String driver = "org.postgresql.Driver";
    private String url = "jdbc:postgresql://localhost:5432/commute_gcm";
    private String username = "splendidbits";
    private String password = "kYBaf34sfd8L";

    @Override
    public EbeanServer get() {
        // Ebean config file.
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUrl(url);
        dataSourceConfig.setHeartbeatTimeoutSeconds(10);
        dataSourceConfig.setDriver(driver);
        dataSourceConfig.setUsername(username);
        dataSourceConfig.setPassword(password);
        dataSourceConfig.setCaptureStackTrace(true);

        ArrayList<Class<?>> models = new ArrayList<Class<?>>();

        // client account models.
        models.add(Account.class);
        models.add(PlatformAccount.class);
        models.add(Platform.class);

        // Agency alert models.
        models.add(Agency.class);
        models.add(Route.class);
        models.add(Alert.class);
        models.add(Location.class);

        // Client device models.
        models.add(Registration.class);
        models.add(Subscription.class);

        // GCM Task models.
        models.add(Message.class);
        models.add(PayloadElement.class);
        models.add(RecipientFailure.class);
        models.add(Recipient.class);
        models.add(Task.class);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setDatabasePlatform(new com.avaje.ebean.config.dbplatform.PostgresPlatform());
        serverConfig.setName(name);
        serverConfig.setDefaultServer(true);
        serverConfig.setRegister(true);
        serverConfig.setClasses(models);
        serverConfig.setDataSourceConfig(dataSourceConfig);

        // Remove missing children.
        serverConfig.setUpdatesDeleteMissingChildren(true);

        return EbeanServerFactory.create(serverConfig);
    }
}