package main;

import java.util.TimeZone;

/**
 * Commute GCM application Constants
 */
public class Constants {
    // Timeout for Agency alerts download requests in MS.
    public static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 5;
    public static final int AGENCY_UPDATE_INTERVAL_SECONDS = 45;

    public static final String COMMUTE_GCM_DB_SERVER = "commute_gcm_server";
    public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");
}
