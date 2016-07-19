package services;

import agency.AgencyAlertModifications;
import dao.AgencyDao;
import enums.AlertType;
import helpers.AlertHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * Receives agency bundles and performs the following actions.
 * <p>
 * 1: Iterate through each Route > Alert bundle and find any differences from the previous set.
 * 3: Collect the new alerts.
 * 4: Persist new data.
 * 5: Get list of subscriptions for route
 * 6: send data in batches of 1000 to google.
 */
public class AlertsUpdateManager {
    private AgencyDao mAgencyService;

    @Inject
    public AlertsUpdateManager(AgencyDao agencyService) {
        mAgencyService = agencyService;
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param agency the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    @Nonnull
    public AgencyAlertModifications getUpdatedRoutesAlerts(@Nonnull Agency agency) {
        AgencyAlertModifications modifiedRouteAlerts = new AgencyAlertModifications(agency.id);

        // Get all new routes, and the current routes that exist for the agency.
        List<Route> freshRoutes = AlertHelper.copyRoute(agency.routes);
        List<Route> existingRoutes = AlertHelper.copyRoute(mAgencyService.getRoutes(agency.id));

        // If there are no existing alerts saved, mark all fetched alerts as new.
        if (freshRoutes != null && existingRoutes.isEmpty()) {
            Logger.info(String.format("Existing routes for agency %s missing. Marked all as updated.", agency.name));

            for (Route freshRoute : freshRoutes) {
                modifiedRouteAlerts.addUpdatedAlerts(freshRoute.alerts);
            }
            return modifiedRouteAlerts;
        }

        // If there are no fetched alerts at all, mark all existing alerts as stale.
        if (freshRoutes == null || freshRoutes.isEmpty()) {
            Logger.info(String.format("New routes for agency %s missing. Marked all as stale.", agency.name));
            for (Route staleRoute : existingRoutes) {
                modifiedRouteAlerts.addUpdatedAlerts(staleRoute.alerts);
            }
            return modifiedRouteAlerts;
        }

        List<Alert> updatedAlerts = new ArrayList<>();
        List<Alert> staleAlerts = new ArrayList<>();
        for (Route freshRoute : freshRoutes) {

            // Iterate through the existing routes and check existing and fresh Routes are the same.
            for (Route existingRoute : existingRoutes) {

                if (freshRoute.routeId.equals(existingRoute.routeId)) {
                    updatedAlerts.addAll(getUpdatedAlerts(existingRoute, existingRoute.alerts, freshRoute.alerts));
                    staleAlerts.addAll(getStaleAlerts(existingRoute, existingRoute.alerts, freshRoute.alerts));

                    // There was a route match so skip the inner loop.
                    break;
                }
            }
        }

        // Remove any alert from the stale alerts where the route type was updated.
        Iterator<Alert> staleAlertsIterator = staleAlerts.iterator();
        while (staleAlertsIterator.hasNext()) {
            Alert staleAlert = staleAlertsIterator.next();

            // If the stale alert routeId is the same as the updated alert, remove the stale alert.
            for (Alert updatedAlert : updatedAlerts) {
                boolean bothRouteIdsExist = updatedAlert.route != null && updatedAlert.route.routeId != null &&
                        staleAlert.route != null && staleAlert.route.routeId != null;

                if (bothRouteIdsExist && updatedAlert.route.routeId.equals(staleAlert.route.routeId)) {
                    staleAlertsIterator.remove();
                    break;
                }
            }
        }

        modifiedRouteAlerts.addUpdatedAlerts(updatedAlerts);
        modifiedRouteAlerts.addStaleAlerts(staleAlerts);
        return modifiedRouteAlerts;
    }

    /**
     * Get a list of fresh (new) alerts that do not exist in the previous (existing) collection.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of updated route alerts.
     */
    @Nonnull
    private List<Alert> getUpdatedAlerts(@Nonnull Route route, List<Alert> existingAlerts, List<Alert> freshAlerts) {
        List<Alert> updatedAlerts = new ArrayList<>();

        if (freshAlerts != null) {
            for (Alert freshAlert : freshAlerts) {

                if (!AlertHelper.isAlertEmpty(freshAlert) &&
                        (existingAlerts == null || existingAlerts.isEmpty() || !existingAlerts.contains(freshAlert))) {
                    Logger.info(String.format("Alert in route %1$s was new or updated.", route.routeId));
                    freshAlert.route = route;
                    updatedAlerts.add(freshAlert);
                }
            }
        }

        return updatedAlerts;
    }

    /**
     * An stale alert is defined as an existing {@link enums.AlertType} for the route which no longer
     * exists in the fresh route alert types.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of stale route alerts.
     */
    @Nonnull
    private List<Alert> getStaleAlerts(@Nonnull Route route, List<Alert> existingAlerts, List<Alert> freshAlerts) {
        List<Alert> staleAlerts = new ArrayList<>();

        // Mark all existing alerts as stale if fresh alerts are empty.
        if (freshAlerts == null || freshAlerts.isEmpty() && existingAlerts != null && !existingAlerts.isEmpty()) {
            return existingAlerts;
        }

        if (existingAlerts != null) {
            Set<AlertType> freshAlertTypes = new HashSet<>();

            // Add all fresh alert types to a set.
            for (Alert freshAlert : freshAlerts) {
                freshAlertTypes.add(freshAlert.type);

                // Just in case the fresh alert is empty, use this iteration to add to stale alerts.
                if (AlertHelper.isAlertEmpty(freshAlert)) {
                    freshAlert.route = route;
                    staleAlerts.add(freshAlert);
                }
            }

            // If an existing alert type does not exist in the updated fresh alerts, it is stale.
            for (Alert existingAlert : existingAlerts) {
                if (!freshAlertTypes.contains(existingAlert.type)) {
                    Logger.info(String.format("Alert in route %s became stale.", route.routeId));
                    staleAlerts.add(existingAlert);
                }
            }
        }
        return staleAlerts;
    }
}
