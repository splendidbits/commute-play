package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Transaction;
import main.Log;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
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
                .eq("id", agency.id)
                .findUnique();

        Transaction transaction = mEbeanServer.beginTransaction();

        try {
            if (existingAgency != null && existingAgency.routes != null) {

                // Loop through each new route and persist.
                for (Route newRoute : agency.routes) {

                    // Find the an existing agency route that matches the agency specific routeId.
                    Route existingRoute = null;
                    for (Route route : agency.routes) {
                        if (route.routeId.equals(newRoute.routeId)) {
                            existingRoute = route;
                            break;
                        }
                    }

                    if (existingRoute != null) {
                        mLog.d(TAG, String.format("Found $1%d existing routes for $2%s.",
                                existingAgency.routes.size(),
                                existingAgency.name));

                        // If something about the new route does not equal the existing route..
                        if (existingRoute.routeId.equals(newRoute.routeId) && !existingRoute.equals(newRoute)) {

                            // Delete all old alerts.
                            mLog.d(TAG, String.format("Deleting $1%d previous alerts for $2%s.",
                                    existingRoute.alerts.size(),
                                    existingRoute.routeName));

                            for (Alert oldAlert : existingRoute.alerts) {
                                mEbeanServer.delete(oldAlert, transaction);
                            }

                            // Update the route properties
                            existingRoute.routeName = newRoute.routeName;
                            existingRoute.agency = agency;
                            existingRoute.alerts = newRoute.alerts;

                            // Delete the alerts for that route
                            mLog.d(TAG, String.format("Saving $1%d alerts for $2%s.",
                                    newRoute.alerts.size(),
                                    existingRoute.routeName));

                            mEbeanServer.save(existingRoute, transaction);
                        }

                    } else {
                        mLog.i(TAG, String.format("Route $1%s for $2%s does not exist.",
                                newRoute.routeId,
                                newRoute.agency.name));

                        newRoute.agency = agency;
                        mEbeanServer.save(newRoute, transaction);
                    }
                }

            } else {
                mLog.i(TAG, String.format("Agency $1%s doesn't exist. Inserting all alerts.", agency.name));
                mEbeanServer.save(agency, transaction);
            }

            // Commit all work.
            transaction.commit();

        } catch (Exception e) {
            mLog.e(TAG, String.format("Error saving agency bundle for %s. Rolling back.", agency.name), e);
            transaction.rollback();
            return false;

        } finally {
            transaction.end();
        }

        return true;
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routeIds to retrieve.
     * @return List of Routes.
     */
    public List<Route> getRouteAlerts(int agencyId, String... routeIds) {
        List<Route> foundRoutes = new ArrayList<>();
        if (routeIds != null) {

            // Loop through the string varargs and find each valid route.
            for (String routeId : routeIds) {
                routeId = routeId.trim().toLowerCase();

                try {
                    Agency agency = mEbeanServer.find(Agency.class)
                            .fetch("routes")
                            .where()
                            .eq("id", agencyId)
                            .eq("routes.id", routeId.toLowerCase())
                            .findUnique();

                    if (agency != null && agency.routes != null) {
                        foundRoutes.add(agency.routes.get(0));
                    }

                } catch (Exception e) {
                    mLog.e(TAG, "Error getting route for routeIds.", e);
                }
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
    public List<Route> getRouteAlerts(int agencyId) {
        try {
            List<Route> routes = mEbeanServer.find(Route.class)
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

            Collections.sort(routes);
            for (Route route : routes) {
                if (route.alerts != null) {
                    Collections.sort(route.alerts);
                }
            }
            return routes;

        } catch (Exception e) {
            mLog.e(TAG, "Error getting routes for agency.", e);
        }
        return null;
    }
}
