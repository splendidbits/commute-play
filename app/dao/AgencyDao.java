package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
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
        Transaction transaction = null;

        boolean modifiedData = false;

        try {
            // Check a previous agency exists / or non-null list of routes for agency.
            if (existingAgency == null || existingAgency.routes == null) {

                // Agency doesn't exist or Routes empty:
                Logger.info(String.format("Agency %s does not exist. Saving all.", newAgency.name));
                transaction = mEbeanServer.createTransaction();

                mEbeanServer.save(newAgency, transaction);
                transaction.commit();
                transaction.end();
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

                        transaction = mEbeanServer.createTransaction();
                        mEbeanServer.insert(freshRoute, transaction);
                        transaction.commit();
                        transaction.end();

                        modifiedData = true;

                    // Existing route_id exists and all children are stale:
                    } else if ((freshRoute.alerts == null || freshRoute.alerts.isEmpty()) &&
                            (existingRouteMatch.alerts != null && !existingRouteMatch.alerts.isEmpty())) {
                        Logger.debug(String.format("Deleting all alerts for route: %s.", freshRoute.routeName));

                        transaction = mEbeanServer.createTransaction();
                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
                                .where()
                                .conjunction()
                                .eq("route.routeId", existingRouteMatch.routeId)
                                .eq("route.id", existingRouteMatch.id)
                                .findList();

                        mEbeanServer.deleteAll(previousAlerts, transaction);
                        transaction.commit();
                        transaction.end();

                        modifiedData = true;

                    // Existing route_id exists and existing alert children are empty and different from Fresh alerts.
                    } else if (!existingRouteMatch.equals(freshRoute) &&
                            (existingRouteMatch.alerts == null || existingRouteMatch.alerts.isEmpty())){
                        Logger.debug(String.format("Saving alert-less route: %s.", freshRoute.routeName));

                        freshRoute.id = existingRouteMatch.id;

                        transaction = mEbeanServer.createTransaction();
                        mEbeanServer.saveAll(freshRoute.alerts, transaction);
                        transaction.commit();
                        transaction.end();

                        modifiedData = true;

                    // Existing route_id exists existing children have been updated:
                    } else if (!existingRouteMatch.equals(freshRoute) && !existingRouteMatch.alerts.isEmpty()) {
                        Logger.debug(String.format("Updating existing alerts for route: %s.", freshRoute.routeName));

                        freshRoute.id = existingRouteMatch.id;

                        transaction = mEbeanServer.createTransaction();
                        List<Alert> previousAlerts = mEbeanServer.find(Alert.class)
                                .where()
                                .conjunction()
                                .eq("route.routeId", existingRouteMatch.routeId)
                                .eq("route.id", existingRouteMatch.id)
                                .findList();

                        mEbeanServer.deleteAllPermanent(previousAlerts, transaction);
                        mEbeanServer.saveAll(freshRoute.alerts, transaction);
                        transaction.commit();
                        transaction.end();

                        modifiedData = true;
                    }
                }
            }

            return modifiedData;

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            if (e.getMessage() != null && e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }
            return false;

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency bundle for %s. Rolling back.", newAgency.name), e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            return false;

        } finally {
            if (transaction != null && transaction.isActive()) {
                transaction.end();
            }
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
        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setReadOnly(true);

        if (routeIds != null) {

            // Loop through the string varargs and find each valid route.
            for (String routeId : routeIds) {
                routeId = routeId.trim().toLowerCase();

                try {
                    Query<Route> query = mEbeanServer.createQuery(Route.class)
                            .setOrder(new OrderBy<>("routeId"))
                            .fetch("agency")
                            .where()
                            .eq("agency.id", agencyId)
                            .eq("routeId", routeId.toLowerCase())
                            .query();

                    return mEbeanServer.findList(query, transaction);

                } catch (PersistenceException e) {
                    Logger.error(String.format("Error fetching routes models from database: %s.", e.getMessage()));

                    if (e.getMessage() != null &&
                            e.getMessage().contains("does not exist") &&
                            e.getMessage().contains("Query threw SQLException:ERROR:")) {
                        createDatabase();
                    }

                } catch (Exception e) {
                    Logger.error("Error getting route for routeIds.", e);

                } finally {
                    transaction.end();
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
        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setReadOnly(true);

        try {
            Query<Route> query = mEbeanServer.createQuery(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency")
                    .fetch("alerts")
                    .where()
                    .eq("agency.id", agencyId)
                    .query();

            return mEbeanServer.findList(query, transaction);

        } catch (PersistenceException e) {
            Logger.error(String.format("Error saving agency model to database: %s.", e.getMessage()));

            if (e.getMessage() != null &&
                    e.getMessage().contains("does not exist") &&
                    e.getMessage().contains("Query threw SQLException:ERROR:")) {
                createDatabase();
            }

        } catch (Exception e) {
            Logger.error("Error getting routes for agency.", e);

        } finally {
            transaction.end();
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
        Transaction transaction = mEbeanServer.createTransaction();
        transaction.setReadOnly(true);

        try {
            Query<Agency> query = mEbeanServer.createQuery(Agency.class)
                    .where()
                    .idEq(agencyId)
                    .query();

            return mEbeanServer.findUnique(query, transaction);

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

        } finally {
            transaction.end();
        }
    }
}
