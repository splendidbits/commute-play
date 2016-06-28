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
    private PushMessageManager mPushMessageManager;

    @Inject
    public AlertsUpdateManager(AgencyDao agencyService, PushMessageManager pushMessageManager) {
        mAgencyService = agencyService;
        mPushMessageManager = pushMessageManager;
    }

    /**
     * Starts of a chain of things when an agency is passed into this method:
     * <p>
     * 1) Builds a list of added and removed alerts for the agency, since the last time.
     * 2) Saves the modified routes in the database.
     * 3) Passes on the alert information to subscribed device / subscription Platform Account clients.
     * 4) Modifies any device information based on the response from Google.
     *
     * @param updatedAgency The agency which has been updated.
     */
    public void processAgencyDownload(Agency updatedAgency) {
        if (updatedAgency != null) {
            AgencyAlertModifications agencyAlertModifications = getUpdatedRoutesAlerts(updatedAgency);

            if (agencyAlertModifications.hasModifiedAlerts()) {
                // Save the agency in the datastore.
                Logger.debug("Saving new or updated agency data.");
                boolean alertsPersisted = mAgencyService.saveAgency(updatedAgency);

                // NOTE: This is a sanity-check to ensure we don't bombard clients with
                // alerts if there's an issue with database persistence.
                if (alertsPersisted) {
                    Logger.debug("New Agency Alerts persisted. Sending to subscribers.");
                    mPushMessageManager.dispatchAlerts(agencyAlertModifications);
                }

            } else {
                Logger.info(String.format("No changed alerts found for agency: %s.", updatedAgency.name));
            }
        }
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param agency the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    @Nonnull
    private AgencyAlertModifications getUpdatedRoutesAlerts(@Nonnull Agency agency) {
        AgencyAlertModifications modifiedRouteAlerts = new AgencyAlertModifications(agency.id);

        // Get all new routes, and the current routes that exist for the agency.
        List<Route> freshRoutes = AlertHelper.copyRoute(agency.routes);
        List<Route> existingRoutes = AlertHelper.copyRoute(mAgencyService.getAgencyRoutes(agency.id));

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

            // If the stale alert type is the same as an updated type, remove the stale alert.
            for (Alert updatedAlert : updatedAlerts) {
                if (updatedAlert.type != null && updatedAlert.type.equals(staleAlert.type)) {
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
     * exists in the fresh route alert yupes.
     *
     * @param existingAlerts The previous stored agency route alerts.
     * @param freshAlerts    The current fresh agency route alerts.
     * @return The list of stale route alerts.
     */
    @Nonnull
    private List<Alert> getStaleAlerts(@Nonnull Route route, List<Alert> existingAlerts, List<Alert> freshAlerts) {

        // Mark all existing alerts as stale if fresh alerts are empty.
        if (freshAlerts == null || freshAlerts.isEmpty()) {
            return existingAlerts;
        }

        List<Alert> staleAlerts = new ArrayList<>();
        if (existingAlerts != null) {
            Set<AlertType> existingTypes = new HashSet<>();
            for (Alert existingAlert : existingAlerts) {
                existingTypes.add(existingAlert.type);
            }

            for (Alert freshAlert : freshAlerts) {
                if (!existingTypes.contains(freshAlert.type) || AlertHelper.isAlertEmpty(freshAlert)) {
                    Logger.info(String.format("Alert in route %s became stale.", route.routeId));
                    freshAlert.route = route;
                    staleAlerts.add(freshAlert);
                }
            }
        }
        return staleAlerts;
    }
}
