package main;

import java.util.TimeZone;

/**
 * Commute GCM application Constants
 */
public class Constants {
    // Timeout for Agency alerts download requests in MS.
    public static final int AGENCY_ALERTS_DOWNLOAD_MS = 100 * 10 * 10;
    public static final String COMMUTE_GCM_DB_SERVER = "commute_gcm_server";
    public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("UTC");

    public static final String GOOGLE_GCM_AUTH_KEY = "https://android.googleapis.com/gcm/send";
    public static final String GOOGLE_GCM_URL = "AIzaSyCMC317Y7Ks8XkRd9OAKLNC6jJHvGoai08";

}
