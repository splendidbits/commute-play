package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Route;
import services.splendidlog.Logger;

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
        boolean persistedAgencyData = false;

        try {
            // Check a previous agency exists / or non-null list of routes for agency.
            if (existingAgency == null || existingAgency.routes == null) {
                // Agency doesn't exist or Routes empty:
                mEbeanServer.beginTransaction();

                Logger.info(String.format("Agency %s does not exist. Saving all.", newAgency.name));
                mEbeanServer.save(newAgency);
                mEbeanServer.commitTransaction();
                return true;

            } else if (newAgency.routes != null) {
                // Agency exists and contains Routes:
                Logger.info(String.format("Agency %s already exists. Checking routes.", newAgency.name));

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

                    if (existingRoute == null) {
                        // Agency route_id does not exist:
                        Logger.debug(String.format("Didn't find existing route: %s.", newRoute.routeName));

                        mEbeanServer.beginTransaction();
                        newRoute.agency = existingAgency;
                        mEbeanServer.save(newRoute);
                        mEbeanServer.commitTransaction();
                        persistedAgencyData = true;

                    } else if (existingRoute.routeId.equals(newRoute.routeId) && !existingRoute.equals(newRoute)) {
                        // Agency route_id exists and is updated:
                        Logger.debug(String.format("Updating route: %s.", newRoute.routeName));

                        mEbeanServer.beginTransaction();
                        newRoute.id = existingRoute.id;
                        mEbeanServer.update(newRoute);
                        mEbeanServer.commitTransaction();
                        persistedAgencyData = true;
                    }
                }
            }

            return persistedAgencyData;

        } catch (PersistenceException e) {
            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
//                createDatabase();
            }
            mEbeanServer.rollbackTransaction();
            return false;

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
            mEbeanServer.rollbackTransaction();
            return false;

        } finally {
            mEbeanServer.endTransaction();
        }
    }

    /**
     * Get all routes for a set of routeIds and an agency name.
     *
     * @param agencyId Id of agency.
     * @param routeIds list of routeIds to retrieve.
     * @return List of Routes.
     */
    @Nullable
    public List<Route> getAgencyRoutes(int agencyId, String... routeIds) {
        mEbeanServer.beginTransaction();

        List<Route> foundRoutes = new ArrayList<>();
        if (routeIds != null) {

            // Loop through the string varargs and find each valid route.
            for (String routeId : routeIds) {
                routeId = routeId.trim().toLowerCase();

                try {
                    return mEbeanServer.find(Route.class)
                            .setOrder(new OrderBy<>("routeId"))
                            .fetch("agency")
                            .where()
                            .eq("agency.id", agencyId)
                            .eq("routeId", routeId.toLowerCase())
                            .findList();

                } catch (PersistenceException e) {
                    if (e.getMessage() != null &&
                            e.getMessage().contains("does not exist") &&
                            e.getMessage().contains("Query threw SQLException:ERROR:")) {
                        createDatabase();
                    }

                } catch (Exception e) {
                    Logger.error("Error getting route for routeIds.", e);

                } finally {
                    mEbeanServer.endTransaction();
                }
            }
        }
        return null;
    }

    /**
     * Get a list of saved alerts for a given agency.
     *
     * @param agencyId id of the agency.
     * @return list of alerts for agency. Can be null.
     */
    @Nullable
    public List<Route> getAgencyRoutes(int agencyId) {
        mEbeanServer.beginTransaction();

        try {
            return mEbeanServer.find(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency")
                    .fetch("alerts")
                    .fetch("alerts.locations")
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

        } catch (PersistenceException e) {
            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);

        } finally {
            mEbeanServer.endTransaction();
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
        mEbeanServer.beginTransaction();

        try {
            return mEbeanServer.find(Agency.class)
                    .where()
                    .eq("id", agencyId)
                    .findUnique();

        } catch (PersistenceException e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }
        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);

        } finally {
            mEbeanServer.endTransaction();
        }
        return null;
    }
}
