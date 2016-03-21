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

public class AgencyServiceDao {
    private static final String TAG = AgencyServiceDao.class.getSimpleName();
    private EbeanServer mEbeanServer;

    /**
     * Get a THREADSAFE Syncronised instance of the main.Log.
     *
     * @return An instance of the AgencyDatabaseService which contain locks.
     */
    private static class Loader {
        static final AgencyServiceDao INSTANCE = new AgencyServiceDao();
    }

    public static AgencyServiceDao getInstance() {
        return Loader.INSTANCE;
    }

    private AgencyServiceDao() {
        try {
            mEbeanServer = Ebean.getServer(Constants.COMMUTE_GCM_DB_SERVER);
        } catch (Exception e) {
            play.Logger.debug("Error setting EBean Datasource properties", e);
        }
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     *
     * @param agency list of route alerts.
     * @return boolean for success.
     */
    public boolean saveAgencyAlerts(@Nonnull Agency agency) {
        // Get the current persisted agency alerts.
        Agency currentAgency = mEbeanServer.find(Agency.class)
                .where()
                .eq("agencyId", agency.agencyId)
                .findUnique();

        mEbeanServer.createTransaction();
        try {
            if (currentAgency != null && currentAgency.routes != null) {

                // Loop through each new route and persist.
                for (Route freshRoute : agency.routes) {
                    Route existingRoute = mEbeanServer.find(Route.class)
                            .fetch("agency")
                            .where()
                            .eq("route_id", freshRoute.routeId)
                            .eq("agency_id", agency.agencyId)
                            .findUnique();

                    if (existingRoute != null) {
                        mEbeanServer.delete(existingRoute.alerts);

                        // Update the route properties
                        existingRoute.routeName = freshRoute.routeName;
                        existingRoute.agency = agency;
                        existingRoute.alerts = freshRoute.alerts;

                        // Delete the alerts for that route
                        mEbeanServer.save(existingRoute);

                    } else {
                        freshRoute.agency = agency;
                        mEbeanServer.save(freshRoute);
                    }
                }

            } else if (currentAgency != null) {
                mEbeanServer.update(agency);

            } else {
                mEbeanServer.save(agency);
            }

            // Commit all work.
            mEbeanServer.commitTransaction();

        } catch (Exception e) {
            Log.e(TAG, String.format("Error saving agency bundle for %s. Rolling back.", agency.agencyName), e);
            mEbeanServer.rollbackTransaction();

        } finally {
            mEbeanServer.endTransaction();
        }
        return false;
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyName Name of agency.
     * @param routeIds   list of routeIds to retrieve.
     * @return List of Routes.
     */
    public List<Route> getRouteAlerts(@Nonnull String agencyName, @Nonnull String... routeIds) {
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
                Log.e(TAG, "Error getting route for routeIds.", e);
            }
        }
        return foundRoutes;
    }

    /**
     * Get a list of saved alerts for a given agency.
     *
     * @param agencyId id of the agency.
     * @return list of alerts for agency. Can be null.
     */
    @Nullable
    public List<Route> getRouteAlerts(@Nonnull int agencyId) {
        try {
            List<Route> existingAlerts = mEbeanServer.find(Route.class)
                    .where()
                    .eq("agency.agencyId", 1)
                    .findList();

            return existingAlerts;

        } catch (Exception e) {
            Log.e(TAG, "Error getting routes for agency.", e);
        }
        return null;
    }

    /**
     * Some ebean query examples.
     */
    private void examples() {
        /*
        Agency currentAgency = mEbeanServer.find(Agency.class)
                .where()
                .eq("agencyId", 1)
                .findUnique();

        List<Route> routesToUpdate = agency.routes;
        */
    }
}
