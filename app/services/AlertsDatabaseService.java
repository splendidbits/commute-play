package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import main.Constants;
import models.alerts.Agency;

import javax.annotation.Nonnull;

public class AlertsDatabaseService {

    private EbeanServer mEbeanServer;
    private static AlertsDatabaseService mInstance;

    /**
     * Get an instance of the main.Log.
     *
     * @return An instance of this logger.
     */
    public static AlertsDatabaseService getInstance() {
        if (mInstance == null) {
            mInstance = new AlertsDatabaseService();
        }

        return mInstance;
    }

    private AlertsDatabaseService() {
        try {
            mEbeanServer = Ebean.getServer(Constants.COMMUTE_GCM_DB_SERVER);

        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
        }
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     *
     * @param agencyRouteAlerts list of route alerts.
     * @return boolean for success.
     */
    public boolean saveRouteAlerts(Agency agencyRouteAlerts) {
        if (agencyRouteAlerts != null) {
            try {
                mEbeanServer.beginTransaction();
                mEbeanServer.delete(Agency.class, agencyRouteAlerts.agencyId);
                mEbeanServer.save(agencyRouteAlerts);
                mEbeanServer.commitTransaction();
                return true;

            } catch (Exception e) {
                mEbeanServer.rollbackTransaction();

            } finally {
                mEbeanServer.endTransaction();
            }
        }
        return false;
    }

    /**
     * Get an alert for a route Id.
     *
     * @param routeId routeId for alert.
     * @return alert.
     */
    public boolean getAlert(@Nonnull String routeId) {
        return Boolean.parseBoolean(null);
    }
}
