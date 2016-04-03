package services;

import com.avaje.ebean.EbeanServer;
import main.Log;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AgencyService {
    private static final String TAG = AgencyService.class.getSimpleName();

    @Inject
    private EbeanServer mEbeanServer;

    @Inject
    private Log mLog;

    @Inject
    public AgencyService(EbeanServer ebeanServer, Log log) {
        mEbeanServer = ebeanServer;
        mLog = log;
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     *
     * @param agency list of route alerts.
     * @return boolean for success.
     */
    public boolean saveAgencyAlerts(@Nonnull Agency agency) {
        // Get the current persisted agency alerts.
        mLog.d(TAG, "Persisting agency routes in database.");
        Agency existingAgency = mEbeanServer.find(Agency.class)
                .where()
                .eq("agencyId", agency.agencyId)
                .findUnique();

        mEbeanServer.createTransaction();

        try {
            if (existingAgency != null && existingAgency.routes != null) {

                // Loop through each new route and persist.
                for (Route freshRoute : agency.routes) {
                    Route existingRoute = mEbeanServer.find(Route.class)
                            .fetch("agency")
                            .where()
                            .eq("route_id", freshRoute.routeId)
                            .eq("agency_id", agency.agencyId)
                            .findUnique();

                    if (existingRoute != null) {
                        mLog.d(TAG, String.format("Found $1%d existing routes for $2%s",
                                existingAgency.routes.size(), existingAgency.agencyName));

                        if (existingRoute.routeId.equals(freshRoute.routeId)) {
                            // Delete all old alerts.
                            mLog.d(TAG, String.format("Found $1%d existing alerts for $2%s .",
                                    existingRoute.alerts.size(), existingRoute.routeName));

                            for (Alert oldAlert : existingRoute.alerts) {
                                mEbeanServer.delete(oldAlert);
                            }

                            // Update the route properties
                            existingRoute.routeName = freshRoute.routeName;
                            existingRoute.agency = agency;
                            existingRoute.alerts = freshRoute.alerts;

                            // Delete the alerts for that route
                            mLog.d(TAG, String.format("Saving $1%d alerts for $2%s .",
                                    freshRoute.alerts.size(), existingRoute.routeName));
                            mEbeanServer.save(existingRoute);
                        }

                    } else {
                        mLog.i(TAG, String.format("Route $1%s for $2%s doesn't exist. Saving new route.",
                                freshRoute.routeName,
                                freshRoute.agency.agencyName));

                        freshRoute.agency = agency;
                        mEbeanServer.save(freshRoute);
                    }
                }

            } else if (existingAgency != null) {
                mLog.i(TAG, String.format("Agency $1%s exists with no routes. Saving all routes.", agency.agencyName));
                mEbeanServer.update(agency);

            } else {
                mLog.i(TAG, String.format("Agency $1%s doesn't exist. Inserting all alerts.", agency.agencyName));
                mEbeanServer.save(agency);
            }

            // Commit all work.
            mEbeanServer.commitTransaction();

        } catch (Exception e) {
            mLog.e(TAG, String.format("Error saving agency bundle for %s. Rolling back.", agency.agencyName), e);
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
                mLog.e(TAG, "Error getting route for routeIds.", e);
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
            mLog.e(TAG, "Error getting routes for agency.", e);
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
