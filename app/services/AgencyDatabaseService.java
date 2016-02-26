package services;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import main.Constants;
import main.Log;
import models.alerts.Agency;
import models.alerts.Route;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AgencyDatabaseService {

    private static EbeanServer mEbeanServer;
    private static AgencyDatabaseService mInstance;

    /**
     * Get an instance of the main.Log.
     *
     * @return An instance of this logger.
     */
    public static AgencyDatabaseService getInstance() {
        if (mInstance == null || mEbeanServer == null) {
            mInstance = new AgencyDatabaseService();
        }

        return mInstance;
    }

    private AgencyDatabaseService() {
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
            mEbeanServer.endTransaction();
            mEbeanServer.beginTransaction();

            try {
                mEbeanServer.delete(Agency.class, agencyRouteAlerts.agencyId);
                mEbeanServer.save(agencyRouteAlerts);
                mEbeanServer.commitTransaction();

            } catch (Exception e) {
                Log.e("Error saving device registration info. Rolling back.", e);
                mEbeanServer.rollbackTransaction();

            } finally {
                mEbeanServer.endTransaction();
            }
            return true;
        }
        return false;
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyName Name of agency.
     * @param routeIds   list of routeIds to retrive.
     * @return List of Routes.
     */
    public List<Route> getRoutes(@Nonnull String agencyName, @Nonnull String... routeIds) {
        List<Route> foundRoutes = new ArrayList<>();
        agencyName = agencyName.trim().toLowerCase();

        // Loop through the string varargs and find each valid route.
        for (String routeId : routeIds) {
            routeId = routeId.trim().toLowerCase();

            try {
                Agency agency = mEbeanServer.find(Agency.class)
                        .fetch("routes")
                        .where()
                        .eq("agency_name", agencyName.toLowerCase())
                        .eq("route_id", routeId.toLowerCase())
                        .findUnique();

                if (agency.routes != null) {
                    foundRoutes.add(agency.routes.get(0));
                }

            } catch (Exception e) {
                Log.e("Error getting route for routeIds.", e);
            }
        }
        return foundRoutes;
    }

    /**
     * Get a list of saved routes for a given agency.
     *
     * @param agencyId id of the agency.
     * @return list of routes.
     */
    @Nullable
    public List<Route> getRoutes(@Nonnull int agencyId) {
        try {
            Agency agency = mEbeanServer.find(Agency.class)
                    .fetch("routes")
                    .where()
                    .eq("agency_id", agencyId)
                    .findUnique();

            return agency.routes;

        } catch (Exception e) {
            Log.e("Error getting routes for agency.", e);
        }
        return null;
    }
}
