package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Transaction;
import main.Log;
import models.alerts.Agency;
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
     * @param newAgency list of route alerts.
     * @return boolean for success.
     */
    public boolean saveAgencyAlerts(@Nonnull Agency newAgency) {
        Collections.sort(newAgency.routes);

        // Start the transaction.
        Transaction transaction = mEbeanServer.beginTransaction();
        transaction.setPersistCascade(true);

        // Fetch [any] the persisted agency that matches the new agency
        mLog.d(TAG, "Persisting agency routes in database.");
        Agency existingAgency = mEbeanServer.find(Agency.class)
                .where()
                .eq("id", newAgency.id)
                .findUnique();

        try {
            // If there is an existing agency with routes, use those ids.
            if (existingAgency != null) {
                Collections.sort(existingAgency.routes);

                // Loop through each route and persist if anything is new.
                for (Route newRoute : newAgency.routes) {

                    // Find an existing route id that matches the updated route.
                    Route existingRoute = null;
                    for (Route route : existingAgency.routes) {
                        if (route.routeId.equals(newRoute.routeId)) {
                            existingRoute = route;
                            break;
                        }
                    }

                    // If a new routeId matched an existing routeId, replace it. If not, replace all
                    // existing route attributes with the new route apart id.
                    if (existingRoute != null) {
                        mLog.d(TAG, String.format("Found $1%d existing routes for $2%s.",
                                existingAgency.routes.size(),
                                existingAgency.name));

                        // If something about the new route does not equal the existing route..
                        if (existingRoute.routeId.equals(newRoute.routeId) &&
                                !existingRoute.equals(newRoute)) {

                            // Delete all old alerts.
                            mLog.d(TAG, String.format("Replacing previous route: $1%s.", existingRoute.routeName));

                            // Assign the existing route id to the new route.
                            newRoute.id = existingRoute.id;

                            // Delete the alerts for that route
                            mLog.d(TAG, String.format("Saving $1%d alerts for $2%s.", newRoute.alerts.size(),
                                    existingRoute.routeId));

                            mEbeanServer.update(newRoute, transaction);
                        }

                    } else {
                        newRoute.agency = newAgency;
                        mEbeanServer.save(newRoute, transaction);
                        mLog.i(TAG, String.format("Agency Route $1%s does not exist. Inserting.", newRoute.routeId));
                    }
                }

            } else {
                // There was no existing agency found locally. Simple save the entire agency.
                mLog.i(TAG, String.format("Agency $1%s doesn't exist. Inserting all alerts.", newAgency.name));
                mEbeanServer.save(newAgency, transaction);
            }

            // Commit all work.
            transaction.commit();
            return true;

        } catch (Exception e) {
            mLog.e(TAG, String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
            transaction.rollback();
            return false;

        } finally {
            transaction.end();
        }
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
                    List<Route> routes = mEbeanServer.find(Route.class)
                            .fetch("routes")
                            .where()
                            .eq("id", agencyId)
                            .eq("routes.routeId", routeId.toLowerCase())
                            .findList();

                    foundRoutes.addAll(routes);

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
                    .setOrder(new OrderBy<>("routeId"))
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

            return routes;

        } catch (Exception e) {
            mLog.e(TAG, "Error getting routes for agency.", e);
        }
        return null;
    }
}
