package dao;

import helpers.AlertHelper;
import io.ebean.EbeanServer;
import io.ebean.ExpressionList;
import io.ebean.FetchConfig;
import io.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agency route / alert database persistence functions.
 */
public class AgencyDao extends BaseDao {

    @Inject
    public AgencyDao(EbeanServer ebeanServer) {
        super(ebeanServer);
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

        // Reset all the models
        AlertHelper.removeInterReferences(newAgency);

        try {
            // Check a previous agency exists / or non-null list of routes for agency.
            if (existingAgency == null || existingAgency.routes == null) {
                // Agency doesn't exist or Routes empty:
                Logger.info(String.format("Agency %s does not exist. Saving all.", newAgency.name));
                mEbeanServer.save(newAgency);

            } else if (newAgency.routes != null) {
                // Agency exists and contains Routes:
                Logger.info(String.format("Agency %s already exists. Checking routes.", newAgency.name));
                Collections.sort(newAgency.routes);

                for (Route freshRoute : newAgency.routes) {
                    // Find an existing route deviceId that matches the updated route.
                    Route existingRoute = null;
                    for (Route route : existingAgency.routes) {
                        if (route.routeId.equals(freshRoute.routeId)) {
                            existingRoute = route;
                            break;
                        }
                    }

                    // Existing route_id does not exist:
                    if (existingRoute == null) {
                        Logger.debug(String.format("Saving new route: %s.", freshRoute.routeName));
                        mEbeanServer.insert(freshRoute);

                    } else if (!existingRoute.equals(freshRoute)) {
                        // Existing alert children are different from Fresh alerts.
                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
                                .fetch("locations")
                                .where()
                                .eq("route.routeId", existingRoute.routeId)
                                .findList();

                        Logger.debug(String.format("Deleting all alerts for route: %s.", freshRoute.routeName));
                        mEbeanServer.deleteAllPermanent(previousAlerts);

                        // If there are new alerts, save them
                        if (freshRoute.alerts != null && !freshRoute.alerts.isEmpty()) {
                            freshRoute.id = existingRoute.id;
                            for (Alert freshAlert : freshRoute.alerts) {
                                freshAlert.route = existingRoute;
                            }
                            mEbeanServer.saveAll(freshRoute.alerts);
                        }

                        // Update the main route attributes (this shouldn't effect the children)
//                        mEbeanServer.update(existingRoute);
                    }
                }

                // Update the main agency attributes (this shouldn't effect the children)
                mEbeanServer.update(newAgency);
            }

            return true;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
        }

        return false;
    }

    /**
     * Get a list of all agencies.
     *
     * @return list of agencies.
     */
    @Nullable
    public List<Agency> getAgencies() {
        return mEbeanServer.find(Agency.class)
                .fetch("routes", new FetchConfig().lazy())
                .findList();
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routesIds to retrieve routes for.
     * @return List of Routes.
     */
    @Nonnull
    public List<Route> getRoutes(int agencyId, @Nonnull List<String> routeIds) {
        List<Route> returnRouteList = new ArrayList<>();

        List<Route> routes = getRoutes(agencyId);
        if (routes != null && !routes.isEmpty()) {

            // Loop through the string varargs..
            for (String requestRoute : routeIds) {
                requestRoute = requestRoute.trim().toLowerCase();

                // ..and find a valid route match.
                for (Route route : routes) {
                    if (route.routeId != null && route.routeId.equals(requestRoute)) {
                        returnRouteList.add(route);
                        break;
                    }
                }
            }
        }
        return returnRouteList;
    }

    /**
     * Get a list of saved alerts for a given agency.
     *
     * @param agencyId id of the agency or null for all agencies.
     * @return list of alerts for agency. Can be null.
     */
    @Nullable
    public List<Route> getRoutes(@Nullable Integer agencyId) {
        try {
            ExpressionList<Route> routesQuery = mEbeanServer.createQuery(Route.class)
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations")
                    .setOrder(new OrderBy<>("routeId"))
                    .setUseCache(false)
                    .where();

            if (agencyId != null) {
                routesQuery.eq("agency.id", agencyId);
            }

            return mEbeanServer.find(Route.class).findList();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));

            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }
        return null;
    }

    @Nullable
    public Route getRoute(int agencyId, @Nonnull String routeId) {
        try {
            return mEbeanServer.find(Route.class)
                    .setUseCache(false)
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations")
                    .where()
                    .conjunction()
                    .eq("agency.id", agencyId)
                    .eq("routeId", routeId)
                    .endJunction()
                    .findUnique();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));

            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

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
                    .setUseCache(false)
                    .fetch("routes", new FetchConfig().query())
                    .fetch("routes.alerts", new FetchConfig().query())
                    .fetch("routes.alerts.locations", new FetchConfig().query())
                    .where()
                    .idEq(agencyId)
                    .query()
                    .findUnique();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching Agency models from database: %s.", e.getMessage()));

            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }
            return null;

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
            return null;
        }
    }

    /**
     * Remove an agency.
     * @param agencyId of agency
     *
     * @return boolean of success.
     */
    public boolean removeAgency(long agencyId) {
        try {
            List<Agency> agencies = mEbeanServer.find(Agency.class)
                    .fetch("routes")
                    .fetch("routes.alerts")
                    .fetch("routes.alerts.locations")
                    .fetch("routes.subscriptions")
                    .where()
                    .idEq(agencyId)
                    .findList();

            if (agencies != null) {
                mEbeanServer.deleteAllPermanent(agencies);
            }
            return true;

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
        }

        return false;
    }
}
