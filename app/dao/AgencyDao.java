package dao;

import helpers.AlertHelper;
import io.ebean.EbeanServer;
import io.ebean.FetchConfig;
import io.ebean.OrderBy;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Location;
import models.alerts.Route;
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
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
     * @param agency new agency to persist.
     * @return boolean for success.
     */
    public synchronized boolean saveAgency(@Nonnull Agency agency) {
        Logger.info("Persisting agency routes in database.");

        AlertHelper.populateBackReferences(agency);

        try {
            Agency savedAgency = mEbeanServer.find(Agency.class)
                    .fetch("routes", new FetchConfig().query())
                    .fetch("routes.alerts", new FetchConfig().query())
                    .fetch("routes.alerts.locations", new FetchConfig().query())
                    .setId(agency.getId())
                    .findOne();

            if (savedAgency == null) {
                mEbeanServer.save(agency);
                return true;
            }

            savedAgency.setName(agency.getName());
            savedAgency.setUtcOffset(agency.getUtcOffset());
            savedAgency.setPhone(agency.getPhone());
            savedAgency.setExternalUri(agency.getExternalUri());
            savedAgency.setRoutes(agency.getRoutes());
            mEbeanServer.save(savedAgency);

        } catch (Exception e) {
            Logger.error(String.format("Error saving agency alerts model to database: %s.", e.getMessage()));
            return false;
        }

        return true;
    }

    /**
     * Remove Alerts and Locations for a route.
     *
     * @param routeId route id to delete children.
     */
    private void removeAlerts(@Nonnull String routeId) {
        try {
            List<Location> locations = mEbeanServer.find(Location.class)
                    .fetch("alert", new FetchConfig().query())
                    .fetch("alert.route", new FetchConfig().query())
                    .where()
                    .eq("alert.route.routeId", routeId)
                    .findList();

            List<Alert> alerts = mEbeanServer.find(Alert.class)
                    .fetch("route", new FetchConfig().query())
                    .where()
                    .eq("route.routeId", routeId)
                    .findList();

            mEbeanServer.deleteAllPermanent(locations);
            mEbeanServer.deleteAllPermanent(alerts);

        } catch (Exception e) {
            Logger.error(String.format("Error deleting alert or location models from database: %s.", e.getMessage()));
        }
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
        try {
            return mEbeanServer.createQuery(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().lazy())
                    .fetch("alerts.locations", new FetchConfig().lazy())
                    .where()
                    .conjunction()
                    .eq("agency.id", agencyId)
                    .in("routeId", routeIds)
                    .endJunction()
                    .findList();

        } catch (Exception e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));
        }

        return new ArrayList<>();
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
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency", new FetchConfig().query())
                    .fetch("alerts", new FetchConfig().lazy())
                    .fetch("alerts.locations", new FetchConfig().lazy())
                    .where()
                    .eq("agency.id", agencyId)
                    .findList();

        } catch (Exception e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));
        }

        return null;
    }

    @Nullable
    public Route getRoute(String agencyId, @Nonnull String routeId) {
        try {
            return mEbeanServer.find(Route.class)
                    .setOrder(new OrderBy<>("routeId"))
                    .fetch("agency", new FetchConfig().lazy())
                    .fetch("alerts", new FetchConfig().lazy())
                    .fetch("alerts.locations", new FetchConfig().lazy())
                    .where()
                    .conjunction()
                    .eq("agency.id", agencyId)
                    .eq("routeId", routeId)
                    .endJunction()
                    .findOne();

        } catch (Exception e) {
            Logger.error(String.format("Error fetching routes model from database: %s.", e.getMessage()));
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
            Agency agency = mEbeanServer.find(Agency.class)
                    .setOrder(new OrderBy<>("routes.routeId desc"))
                    .fetch("routes", new FetchConfig().lazy())
                    .fetch("routes.alerts", new FetchConfig().lazy())
                    .fetch("routes.alerts.locations", new FetchConfig().lazy())
                    .where()
                    .idEq(agencyId)
                    .query()
                    .findOne();

            return agency;

        } catch (Exception e) {
            Logger.error(String.format("Error fetching Agency models from database: %s.", e.getMessage()));
        }

        return null;
    }

    public void removeAgency(String agencyId) {
        try {
            List<Agency> agencies = mEbeanServer.find(Agency.class)
                    .fetch("routes")
                    .fetch("routes.alerts")
                    .fetch("routes.alerts.locations")
                    .where()
                    .idEq(agencyId)
                    .findList();

            mEbeanServer.deleteAllPermanent(agencies);

        } catch (Exception e) {
            Logger.error(String.format("Error deleting Agency model from database: %s.", e.getMessage()));
        }
    }
}
