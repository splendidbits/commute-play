package dao;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

import io.ebean.EbeanServer;
import io.ebean.FetchConfig;
import io.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.Logger;

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
    public synchronized boolean saveAgency(@Nonnull Agency freshAgency) {
        Logger.info("Persisting agency routes in database.");
        final Agency savedAgency = getAgency(freshAgency.getId());

        try {
            if (savedAgency == null) {
                mEbeanServer.save(freshAgency);
                return true;
            }

            // Delete all routes if there are no fresh routes.
            if (CollectionUtils.isEmpty(freshAgency.getRoutes())) {
                mEbeanServer.deleteAllPermanent(savedAgency.getRoutes());

            } else {
                for (Route freshRoute : freshAgency.getRoutes()) {
                    boolean foundRouteIdMatch = false;

                    // If there's a route ID match.
                    for (Route savedRoute : savedAgency.getRoutes()) {
                        if (freshRoute.getRouteId().equals(savedRoute.getRouteId())) {
                            foundRouteIdMatch = true;

                            // fresh and saved routes are different, delete existing alerts.
                            if (savedRoute.getAlerts() != null && !savedRoute.getAlerts().equals(freshRoute.getAlerts())) {
                                mEbeanServer.deleteAllPermanent(savedRoute.getAlerts());
                            }

                            // fresh and saved routes are different, save new alerts.
                            if (freshRoute.getAlerts() != null && !freshRoute.getAlerts().equals(savedRoute.getAlerts())) {
                                for (Alert freshAlert : freshRoute.getAlerts()) {
                                    freshAlert.setRoute(freshRoute);
                                    mEbeanServer.save(freshAlert);
                                }
                            }

                            // Update other route properties.
                            if (!freshRoute.equals(savedRoute)) {
                                freshRoute.setAgency(freshAgency);
                                mEbeanServer.update(freshRoute);
                            }

                            // Saved route exists for fresh route so break saved routes loop.
                            break;
                        }
                    }

                    if (!foundRouteIdMatch) {
                        freshRoute.setAgency(freshAgency);
                        mEbeanServer.save(freshRoute);
                    }
                }

                if (!freshAgency.equals(savedAgency)) {
                    mEbeanServer.update(freshAgency);
                }
            }

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            return false;

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", freshAgency.getName()), e);
            return false;
        }

        return true;
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routesIds to retrieve routes for.
     * @return List of Routes.
     */
    @Nonnull
    public List<Route> getRoutes(String agencyId, @Nonnull List<String> routeIds) {
        List<Route> returnRouteList = new ArrayList<>();

        List<Route> routes = getRoutes(agencyId);
        if (routes != null && !routes.isEmpty()) {

            // Loop through the string varargs..
            for (String requestRoute : routeIds) {

                // ..and find a valid route match.
                for (Route route : routes) {
                    if (!StringUtils.isEmpty(route.getRouteId())
                            && route.getRouteId().trim().toLowerCase().equals(
                                    requestRoute.trim().toLowerCase())) {
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
    public List<Route> getRoutes(String agencyId) {
        try {
            return mEbeanServer.createQuery(Route.class)
                    .setOrder(new OrderBy<>("routeId desc"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().query())
                    .fetch("alerts.locations", new FetchConfig().query())
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);
        }

        return null;
    }

    @Nullable
    public Route getRoute(String agencyId, @Nonnull String routeId) {
        try {
            List<Route> routes = mEbeanServer.find(Route.class)
                    .setOrder(new OrderBy<>("routeId descc"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().query())
                    .fetch("alerts.locations", new FetchConfig().query())
                    .where()
                    .conjunction()
                    .eq("agency.id", agencyId)
                    .eq("routeId", routeId)
                    .endJunction()
                    .findList();

            if (!routes.isEmpty()) {
                return routes.get(routes.size() - 1);
            }

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));

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
    public synchronized Agency getAgency(String agencyId) {
        try {
            return mEbeanServer.find(Agency.class)
                    .setOrder(new OrderBy<>("routes.routeId desc"))
                    .fetch("routes", new FetchConfig().query())
                    .fetch("routes.alerts", new FetchConfig().query())
                    .fetch("routes.alerts.locations", new FetchConfig().query())
                    .where()
                    .idEq(agencyId)
                    .query()
                    .findOne();

        } catch (PersistenceException e) {
            Logger.error(String.format("Error fetching Agency models from database: %s.", e.getMessage()));

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

    public synchronized boolean removeAgency(String agencyId) {
        try {
            List<Agency> agencies = mEbeanServer.find(Agency.class)
                    .fetch("routes")
                    .fetch("routes.alerts")
                    .fetch("routes.alerts.locations")
                    .where()
                    .idEq(agencyId)
                    .findList();

            mEbeanServer.deleteAll(agencies);

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
            return false;
        }

        return true;
    }
}
