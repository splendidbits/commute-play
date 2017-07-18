package dao;

import helpers.AlertHelper;
import helpers.CompareUtils;
import io.ebean.EbeanServer;
import io.ebean.ExpressionList;
import io.ebean.FetchConfig;
import io.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Route;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
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
                Logger.info(String.format("Agency %s does not exist. Saving all.", newAgency.name));
                mEbeanServer.save(newAgency);

            } else if (newAgency.routes != null) {
                Logger.info(String.format("Agency %s already exists. Checking routes.", newAgency.name));

                // Find an existing route deviceId that matches the updated route.
                for (Route freshRoute : newAgency.routes) {
                    Route existingRoute = null;

                    for (Route potentialRoute : existingAgency.routes) {
                        if (potentialRoute.routeId.equals(freshRoute.routeId)) {
                            freshRoute.id = potentialRoute.id;
                            freshRoute.agency = potentialRoute.agency;
                            existingRoute = potentialRoute;
                            break;
                        }
                    }

                    if (existingRoute == null) {
                        // Existing route_id does not exist:
                        Logger.debug(String.format("Saving new route: %s.", freshRoute.routeName));
                        mEbeanServer.save(freshRoute);

                    } else if (!freshRoute.equals(existingRoute)) {
                        // Existing alert children are different from Fresh alerts.
                        freshRoute.id = existingRoute.id;

                        Logger.debug(String.format("Deleting all alerts for route: %s.", existingRoute.routeName));
                        mEbeanServer.deleteAll(existingRoute.alerts);
                        mEbeanServer.saveAll(freshRoute.alerts);


//                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
//                                .fetch("locations")
//                                .where()
//                                .eq("route.routeId", existingRoute.routeId)
//                                .findList();
//
//                        Logger.debug(String.format("Deleting all alerts for route: %s.", existingRoute.routeName));
//                        mEbeanServer.deleteAllPermanent(previousAlerts);
//
//                        // If there are new alerts, save them
//                        if (freshRoute.alerts != null) {
//
//                            // Add the relations for corresponding models.
//                            for (Alert freshAlert : freshRoute.alerts) {
//                                freshAlert.route = freshRoute;
//                                for (Location location : freshAlert.locations) {
////                                    location.alert = freshAlert;
//                                }
//                            }
//                            mEbeanServer.saveAll(freshRoute.alerts);
//                        }
                    }
                }

                // Update the main agency attributes (this shouldn't effect the children)
                mEbeanServer.save(newAgency);
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
                .setUseCache(false)
                .fetch("routes", new FetchConfig().query())
                .fetch("routes.alerts", new FetchConfig().query())
                .fetch("routes.alerts.locations", new FetchConfig().query())
                .setOrderBy(new OrderBy<>("route.routeId desc"))
                .findList();
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routesIds to retrieve routes for.
     * @return List of Routes.
     */
    @Nullable
    public List<Route> getRoutes(int agencyId, @Nonnull List<String> routeIds) {
        List<Route> routes = getRoutes(agencyId);

        // Loop through the routes and find a valid route match.
        List<Route> returnRouteList = new ArrayList<>();
        for (String requestRoute : routeIds) {

            for (Route route : routes) {
                if (CompareUtils.isEquals(requestRoute, route.routeId)) {
                    returnRouteList.add(route);
                    break;
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
    @Nonnull
    public List<Route> getRoutes(@Nullable Integer agencyId) {
        try {
            ExpressionList<Route> routesQuery = mEbeanServer.createQuery(Route.class)
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations")
                    .setOrder(new OrderBy<>("routeId desc"))
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

        return new ArrayList<>();
    }

    @Nullable
    public Route getRoute(int agencyId, @Nonnull String routeId) {
        try {
            return mEbeanServer.find(Route.class)
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
                    .setOrderBy(new OrderBy<>("routes.routeId desc"))
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

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }
        return null;
    }

    /**
     * Remove an agency.
     *
     * @param agencyId of agency
     * @return boolean of success.
     */
    public boolean removeAgency(long agencyId) {
        try {
            List<Agency> agencies = mEbeanServer.find(Agency.class)
                    .fetch("routes")
                    .fetch("routes.alerts")
                    .fetch("routes.alerts.locations")
                    .where()
                    .idEq(agencyId)
                    .findList();

            if (agencies != null) {
                mEbeanServer.deleteAllPermanent(agencies);
                return true;
            }

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
        }
        return false;
    }
}
