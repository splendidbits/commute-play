package main;

import models.ILogConfiguration;

import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Sample splendid-log configuration.
 */
public class LogConfig extends ILogConfiguration {

    @Override
    public TimeZone getLogTimezone() {
        return new SimpleTimeZone(0, "UTC");
    }

    @Override
    public String getClientAppName() {
        return "gcm_main";
    }

    @Override
    public String getDataStoreUsername() {
        return "splendidbits";
    }

    @Override
    public String getDataStorePassword() {
        return "kYBOB7@338h!L";
    }

    @Override
    public String getDataStoreDriver() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getDataStoreUrl() {
        return "jdbc:postgresql://localhost/splendidlog";
    }
}