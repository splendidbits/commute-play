package dao;

import io.ebean.EbeanServer;
import io.ebean.ExpressionList;
import io.ebean.FetchConfig;
import io.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
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
     * @param freshAgency new agency to persist.
     * @return boolean for success.
     */
    public boolean saveAgency(@Nonnull Agency freshAgency) {
        Logger.debug("Persisting agency routes in database.");
        final Agency existingAgency = getAgency(freshAgency.id);

        try {
            if (existingAgency == null) {
                mEbeanServer.insert(freshAgency);
                return true;
            }

            // Find routes / alerts that need updated.
            if (freshAgency.routes != null) {
                for (Route freshRoute : freshAgency.routes) {
                    boolean matchingRouteFound = false;
                    freshRoute.agency = freshAgency;

                    // Find an existing <-> fresh routeId match.
                    for (Route existingRoute : existingAgency.routes) {
                        matchingRouteFound = existingRoute.routeId.equals(freshRoute.routeId);
                        boolean routeContentsMismatch = !existingRoute.equals(freshRoute);

                        // Match found. copy the routeId.
                        if (matchingRouteFound && routeContentsMismatch) {
                            freshRoute.id = existingRoute.id;

                            // Delete existing alerts
                            if (existingRoute.alerts != null) {
                                mEbeanServer.deleteAll(existingRoute.alerts);
                            }

                            // Insert each alert.
                            if (freshRoute.alerts != null) {

                                for (Alert freshAlert : freshRoute.alerts) {
                                    freshAlert.route = freshRoute;
                                    mEbeanServer.insert(freshAlert);

                                    // Insert each location.
                                    if (freshAlert.locations != null) {
                                        for (Location freshLocation : freshAlert.locations) {
                                            freshLocation.alert = freshAlert;
                                            mEbeanServer.insert(freshLocation);
                                        }
                                    }
                                }
                            }

                            // Save any other changes in the route.
                            mEbeanServer.update(freshRoute);

                            // A match was found so go to the next freshRoute.
                            break;
                        }
                    }
                    // If no matching route was found, insert it.
                    if (!matchingRouteFound) {
                        mEbeanServer.insert(freshRoute);
                    }
                }
            }

            // Find stale route alerts that need deleted.
            if (existingAgency.routes != null) {
                for (Route existingRoute : existingAgency.routes) {
                    boolean matchingRouteFound = false;

                    if (freshAgency.routes != null) {
                        for (Route freshRoute : freshAgency.routes) {
                            if (freshRoute.routeId.equals(existingRoute.routeId)) {
                                matchingRouteFound = true;
                                break;
                            }
                        }
                    }
                    // No new route was found that matches the existing routeId. Delete just the alerts.
                    if (!matchingRouteFound) {
                        mEbeanServer.delete(existingRoute);
                    }
                }
            }

            if (freshAgency.routes == null || freshAgency.routes.isEmpty()) {
                mEbeanServer.deleteAll(existingAgency.routes);
            }

            // Save any changes in the agency.
            mEbeanServer.update(freshAgency);
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
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations").where();

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
