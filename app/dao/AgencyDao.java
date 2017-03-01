package dao;

import com.avaje.ebean.*;
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
        mEbeanServer.markAsDirty(newAgency);
        boolean modifiedData = false;

        try {
            // Check a previous agency exists / or non-null list of routes for agency.
            if (existingAgency == null || existingAgency.routes == null) {

                // Agency doesn't exist or Routes empty:
                Logger.info(String.format("Agency %s does not exist. Saving all.", newAgency.name));
                mEbeanServer.save(newAgency);
                modifiedData = true;

            } else if (newAgency.routes != null) {

                // Agency exists and contains Routes:
                Logger.info(String.format("Agency %s already exists. Checking routes.", newAgency.name));
                Collections.sort(newAgency.routes);

                for (Route freshRoute : newAgency.routes) {

                    // Find an existing route deviceId that matches the updated route.
                    Route existingRouteMatch = null;
                    for (Route route : existingAgency.routes) {
                        if (route.routeId.equals(freshRoute.routeId)) {
                            existingRouteMatch = route;
                            break;
                        }
                    }

                    // Existing route_id does not exist:
                    if (existingRouteMatch == null) {
                        Logger.debug(String.format("Saving new route: %s.", freshRoute.routeName));
                        freshRoute.agency = existingAgency;

                        mEbeanServer.insert(freshRoute);
                        modifiedData = true;

                        // Existing route_id exists and all children are stale:
                    } else if ((freshRoute.alerts == null || freshRoute.alerts.isEmpty()) &&
                            (existingRouteMatch.alerts != null && !existingRouteMatch.alerts.isEmpty())) {

                        Logger.debug(String.format("Deleting all alerts for route: %s.", freshRoute.routeName));
                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
                                .where()
                                .conjunction()
                                .eq("route.routeId", existingRouteMatch.routeId)
                                .eq("route.id", existingRouteMatch.id)
                                .findList();

                        mEbeanServer.deleteAll(previousAlerts);
                        modifiedData = true;

                        // Existing route_id exists and existing alert children are empty and different from Fresh alerts.
                    } else if (!existingRouteMatch.equals(freshRoute) &&
                            (existingRouteMatch.alerts == null || existingRouteMatch.alerts.isEmpty())) {

                        Logger.debug(String.format("Saving alert-less route: %s.", freshRoute.routeName));
                        freshRoute.id = existingRouteMatch.id;

                        mEbeanServer.saveAll(freshRoute.alerts);
                        modifiedData = true;

                        // Existing route_id exists existing children have been updated:
                    } else if (!existingRouteMatch.equals(freshRoute) && !existingRouteMatch.alerts.isEmpty()) {

                        Logger.debug(String.format("Updating existing alerts for route: %s.", freshRoute.routeName));
                        freshRoute.id = existingRouteMatch.id;

                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
                                .where()
                                .conjunction()
                                .eq("route.routeId", existingRouteMatch.routeId)
                                .eq("route.id", existingRouteMatch.id)
                                .findList();

                        mEbeanServer.deleteAllPermanent(previousAlerts);
                        mEbeanServer.saveAll(freshRoute.alerts);
                        modifiedData = true;
                    }
                }
            }

            return modifiedData;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }
            return false;

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
            return false;
        }
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
                    .fetch("routes")
                    .fetch("routes.alerts")
                    .fetch("routes.alerts.locations")
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
}
