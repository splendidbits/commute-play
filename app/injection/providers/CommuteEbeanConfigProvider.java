package injection.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import io.ebean.config.ServerConfig;
import main.Constants;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import models.devices.Device;
import models.devices.Subscription;
import play.Environment;
import play.db.DBApi;
import play.db.ebean.EbeanConfig;
import play.inject.ApplicationLifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 25/03/2018 Splendid Bits.
 */
public class CommuteEbeanConfigProvider implements Provider<EbeanConfig> {
    private final ServerConfig serverConfig;

    @Inject
    public CommuteEbeanConfigProvider(Config config, ApplicationLifecycle lifecycle, Environment environment, DBApi dbApi) {
        if (config == null || config.isEmpty()) {
            throw new RuntimeException("No Play Framework configuration found.");
        }

        Config configuration = config.getConfig("db." + Constants.DATABASE_SERVER_NAME);
        if (configuration == null) {
            throw new RuntimeException("No commutealerts configuration found");
        }

        // Build custom properties from main configuration.
        Properties properties = new Properties();
        for (Map.Entry<String, ConfigValue> configEntry : configuration.entrySet()) {
            String value = configEntry.getValue().render();
            if (configEntry.getValue().valueType().equals(ConfigValueType.STRING)) {
                value = (String) configEntry.getValue().unwrapped();
            }
            properties.put(configEntry.getKey(), value);
        }

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
        serverConfig.loadFromProperties(properties);

        serverConfig.setRegister(true);
        serverConfig.setDefaultServer(true);
        serverConfig.setUpdatesDeleteMissingChildren(false);
        serverConfig.setClasses(models);
        serverConfig.setDdlGenerate(false);
        serverConfig.setUpdateChangesOnly(false);

        this.serverConfig = serverConfig;
    }

    @Override
    public EbeanConfig get() {
        return new EbeanConfig() {
            @Override
            public String defaultServer() {
                return serverConfig.getName();
            }

            @Override
            public Map<String, ServerConfig> serverConfigs() {
                return Collections.singletonMap(serverConfig.getName(), serverConfig);
            }
        };
    }
}
