package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Route;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agency route / alert database storage functions.
 */
public class AgencyDao {
    private static final String TAG = AgencyDao.class.getSimpleName();

    @Inject
    private EbeanServer mEbeanServer;

    @Inject
    public AgencyDao(EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     *
     * @param newAgency list of route alerts.
     * @return boolean for success.
     */
    public boolean saveAgencyAlerts(@Nonnull Agency newAgency) {
        Collections.sort(newAgency.routes);

        // Fetch [any] the persisted agency that matches the new agency
        Logger.debug("Persisting agency routes in database.");
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

                    // Find an existing route deviceId that matches the updated route.
                    Route existingRoute = null;
                    for (Route route : existingAgency.routes) {
                        if (route.routeId.equals(newRoute.routeId)) {
                            existingRoute = route;
                            break;
                        }
                    }

                    // If a new routeId matched an existing routeId, replace it. If not, replace all
                    // existing route attributes with the new route apart deviceId.
                    if (existingRoute != null) {
                        Logger.debug(String.format("Found $1%d existing routes for $2%s.",
                                existingAgency.routes.size(),
                                existingAgency.name));

                        // If something about the new route does not equal the existing route..
                        if (existingRoute.routeId.equals(newRoute.routeId) && !existingRoute.equals(newRoute)) {

                            // Delete all old alerts.
                            Logger.debug(String.format("Replacing previous route: %s.", existingRoute.routeName));

                            // Assign new attributes to the found routeId.
                            existingRoute.routeId = newRoute.routeId;
                            existingRoute.routeName = newRoute.routeName;
                            existingRoute.agency = newRoute.agency;
                            existingRoute.alerts = newRoute.alerts;
                            existingRoute.subscriptions = newRoute.subscriptions;
                            existingRoute.transitType = newRoute.transitType;
                            existingRoute.routeFlag = newRoute.routeFlag;
                            existingRoute.isSticky = newRoute.isSticky;
                            existingRoute.isDefault = newRoute.isDefault;
                            existingRoute.externalUri = newRoute.externalUri;

                            // Delete the alerts for that route
                            Logger.debug(String.format("Saving %d alerts for %s.",
                                    newRoute.alerts != null ? newRoute.alerts.size() : 0, existingRoute.routeId));

                            mEbeanServer.update(existingRoute);
                        }

                    } else {
                        newRoute.agency = newAgency;
                        mEbeanServer.save(newRoute);
                        Logger.info(String.format("Agency Route %s does not exist. Inserting.", newRoute.routeId));
                    }
                }

            } else {
                // There was no existing agency found locally. Simple save the entire agency.
                Logger.info(String.format("Agency %s doesn't exist. Inserting all alerts.", newAgency.name));
                mEbeanServer.save(newAgency);
            }
            return true;

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
            return false;
        }
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routeIds to retrieve.
     * @return List of Routes.
     */
    public List<Route> getAgencyRoutes(int agencyId, String... routeIds) {
        List<Route> foundRoutes = new ArrayList<>();
        if (routeIds != null) {

            // Loop through the string varargs and find each valid route.
            for (String routeId : routeIds) {
                routeId = routeId.trim().toLowerCase();

                try {
                    List<Route> routes = mEbeanServer.find(Route.class)
                            .setOrder(new OrderBy<>("routeId"))
                            .fetch("agency")
                            .where()
                            .eq("agency.id", agencyId)
                            .eq("routeId", routeId.toLowerCase())
                            .findList();

                    foundRoutes.addAll(routes);

                } catch (Exception e) {
                    Logger.error("Error getting route for routeIds.", e);
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
    public List<Route> getAgencyRoutes(int agencyId) {
        List<Route> routes = new ArrayList<>();
        try {
            List<Route> fetchedRoutes = mEbeanServer.find(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency")
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

            if (fetchedRoutes != null){
                routes.addAll(fetchedRoutes);
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }
        return routes;
    }
}
