package dao;

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

    private void insertAlerts(Route route) {
        // Save each alert.
        if (route.alerts != null) {
            for (Alert freshAlert : route.alerts) {
                freshAlert.route = route;
                mEbeanServer.save(freshAlert);
            }
        }
    }

    private void insertAgency(Agency agency) {
        if (agency != null) {
            mEbeanServer.save(agency);
            insertRoutes(agency, agency.routes);
        }
    }

    private void insertRoutes(Agency agency, List<Route> routes) {
        if (routes != null) {
            for (Route route : routes) {
                route.agency = agency;

                // Save top level changes in the route.
                mEbeanServer.save(route);

                insertAlerts(route);
            }
        }
    }

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     *
     * @param freshAgency new agency to persist.
     * @return boolean for success.
     */
    public boolean saveAgency(@Nonnull Agency freshAgency) {
        Logger.debug("Persisting agency routes in database.");
        final Agency existingAgency = getAgency(freshAgency.id);

        try {
            if (existingAgency == null) {
                insertAgency(freshAgency);
                return true;
            }

            // Delete all routes if there are no fresh routes.
            if (freshAgency.routes == null || freshAgency.routes.isEmpty()) {
                mEbeanServer.deleteAllPermanent(existingAgency.routes);
            }

            if (freshAgency.routes != null) {

                // If there are no existing routes at all, just insert all.
                if (existingAgency.routes == null || existingAgency.routes.isEmpty()) {
                    insertRoutes(freshAgency, freshAgency.routes);
                    return true;
                }

                // Find route matches.
                for (Route freshRoute : freshAgency.routes) {
                    boolean foundExistingRouteMatch = false;

                    for (Route existingRoute : existingAgency.routes) {

                        // If there's a route ID match.
                        if (freshRoute.routeId.equals(existingRoute.routeId) && !foundExistingRouteMatch) {

                            // Existing and fresh routes have different contents.
                            if (!freshRoute.equals(existingRoute)) {
                                freshRoute.id = existingRoute.id;

                                // The alerts have changed.
                                if (freshRoute.alerts != null) {

                                    if (!freshRoute.alerts.equals(existingRoute.alerts)) {

                                        // Delete all existing alerts.
                                        if (existingRoute.alerts != null) {
                                            mEbeanServer.deleteAllPermanent(existingRoute.alerts);

                                            // Delete all existing locations.
                                            for (Alert existingAlert : existingRoute.alerts) {
                                                mEbeanServer.deleteAllPermanent(existingAlert.locations);
                                            }
                                        }

                                        // Save the new alerts.
                                        insertAlerts(freshRoute);
                                    }
                                }
                            }

                            // Update the base properties of a route.
                            mEbeanServer.update(Route.class)
                                    .set("routeName", freshRoute.routeName)
                                    .set("transitType", freshRoute.transitType)
                                    .set("routeFlag", freshRoute.routeFlag)
                                    .set("isDefault", freshRoute.isDefault)
                                    .set("isSticky", freshRoute.isSticky)
                                    .where()
                                    .conjunction()
                                    .eq("routeId", freshRoute.routeId)
                                    .eq("agency.id", freshAgency.id)
                                    .update();

                            // Mark that we have found a route match.
                            foundExistingRouteMatch = true;
                        }
                    }

                    if (!foundExistingRouteMatch) {
                        insertRoutes(freshAgency, Collections.singletonList(freshRoute));
                    }
                }

                // Update the base properties of an Agency
                mEbeanServer.update(Agency.class)
                        .set("name", freshAgency.name)
                        .set("phone", freshAgency.phone)
                        .set("externalUri", freshAgency.externalUri)
                        .set("utcOffset", freshAgency.utcOffset)
                        .where()
                        .idEq(freshAgency.id)
                        .update();
            }

            return true;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", freshAgency.name), e);
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
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().query())
                    .fetch("alerts.locations", new FetchConfig().query())
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
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().query())
                    .fetch("alerts.locations", new FetchConfig().query())
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
                    .setOrder(new OrderBy<>("routes.routeId desc"))
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
                mEbeanServer.deleteAll(agencies);
                return true;
            }

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
        }
        return false;
    }
}
