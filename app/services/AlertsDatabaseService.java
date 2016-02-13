package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import models.alerts.Agency;
import play.db.ebean.Transactional;

public class AlertsDatabaseService {

    private EbeanServer mEbeanServer;
    private static AlertsDatabaseService mInstance;

    /**
     * Get an instance of the main.Log.
     *
     * @return An instance of this logger.
     */
    public static AlertsDatabaseService getInstance(){
        if (mInstance == null) {
            mInstance = new AlertsDatabaseService();
        }

        return mInstance;
    }

    private AlertsDatabaseService() {
        try {
            mEbeanServer = Ebean.getServer("route_alerts");

        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
        }
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     * @param agencyRouteAlerts list of route alerts.
     * @return boolean for success.
     */
    @Transactional
    public boolean saveRouteAlerts(Agency agencyRouteAlerts) {
        if (agencyRouteAlerts != null) {
            try {
                mEbeanServer.beginTransaction();
                mEbeanServer.delete(Agency.class, agencyRouteAlerts.id);
                mEbeanServer.save(agencyRouteAlerts);
                return true;

            } catch (Exception e) {
                mEbeanServer.rollbackTransaction();

            } finally {
                mEbeanServer.endTransaction();
            }
        }
        return false;
    }

}
