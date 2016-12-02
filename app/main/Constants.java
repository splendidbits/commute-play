package main;

/**
 * Commute GCM application Constants
 */
public class Constants {
    public final static String API_SERVER_HOST = "https://commuteapp.io/api";
    public final static String DATABASE_SERVER_NAME = "commute_gcm";

    // Timeout for Agency alerts download requests in MS.
    public static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 10;
    public static final int AGENCY_UPDATE_INTERVAL_SECONDS = 45;

    // Controls if the DDL database scripts be generated on run.
    public static final boolean GENERATE_RUN_DLL_DATABASE = false;
}
