package main;

/**
 * Commute GCM application Constants
 */
public class Constants {
    public static final String PROD_API_SERVER_HOST = "https://api.commuteapp.io";
    public static final String DEBUG_API_SERVER_HOST = "https://localhost:4343";
    public static final String DATABASE_SERVER_NAME = "commutealerts";
    public static final String DATABASE_TYPE = "postgres";

    // Timeout for Agency alerts download requests in MS.
    public static final int AGENCY_UPDATE_INITIAL_DELAY_SECONDS = 10;
    public static final int AGENCY_UPDATE_INTERVAL_SECONDS = 45;

    // Controls if the DDL database scripts be generated on run.
    public static final boolean GENERATE_RUN_DLL_DATABASE = false;
}
