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
 * Agency route / alert database persistence functions.
 */
public class AgencyDao {

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
    public boolean saveAgency(@Nonnull Agency newAgency) {
        Logger.debug("Persisting agency routes in database.");
        Agency existingAgency = getAgency(newAgency.id);
        boolean persistedAgencyData = false;

        try {
            // Check a previous agency exists / or non-null list of routes for agency.
            if (existingAgency == null || existingAgency.routes == null) {
                Logger.info(String.format("Agency %d does not exist. Saving all.", newAgency.id));
                mEbeanServer.save(newAgency);
                return true;

            } else if (newAgency.routes != null) {
                Logger.info(String.format("Agency %d already exists. Checking routes.", newAgency.id));
                Collections.sort(newAgency.routes);

                for (Route newRoute : newAgency.routes) {
                    Route existingRoute = null;

                    // Find an existing route deviceId that matches the updated route.
                    for (Route route : existingAgency.routes) {
                        if (route.routeId.equals(newRoute.routeId)) {
                            existingRoute = route;
                            break;
                        }
                    }

                    // If an existing route for the new route does not exist, save it.
                    if (existingRoute == null) {
                        Logger.debug(String.format("Didn't find existing route: %s.", newRoute.routeName));
                        newRoute.agency = existingAgency;
                        mEbeanServer.save(newRoute);
                        persistedAgencyData = true;

                        // If it exists, but it is not the same, update it.
                    } else if (existingRoute.routeId.equals(newRoute.routeId) && !existingRoute.equals(newRoute)) {
                        Logger.debug(String.format("Updating route: %s.", newRoute.routeName));
                        newRoute.id = existingRoute.id;
                        mEbeanServer.update(newRoute);
                        persistedAgencyData = true;
                    }
                }
            }

            return persistedAgencyData;

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
        try {
            return mEbeanServer.find(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations")
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }
        return null;
    }

    /**
     * Get a s saved agency and all children.
     *
     * @param agencyId id of the agency.
     * @return agency model with children, if found, or null.
     */
    @Nullable
    public Agency getAgency(int agencyId) {
        try {
            return mEbeanServer.find(Agency.class)
                    .where()
                    .eq("id", agencyId)
                    .findUnique();

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }
        return null;
    }
}
