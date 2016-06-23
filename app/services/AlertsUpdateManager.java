package services;

import agency.AgencyModifications;
import dao.AgencyDao;
import enums.AlertType;
import helpers.CommuteAlertHelper;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void saveAndNotifyAgencySubscribers(Agency updatedAgency) {
        if (updatedAgency != null) {
            AgencyModifications agencyModifications = getUpdatedRoutesAlerts(updatedAgency);

            if (agencyModifications.hasModifiedRoutes()) {
                // Save the agency in the datastore.
                Logger.debug("Saving new or updated agency data.");
                boolean alertsPersisted = mAgencyService.saveAgency(updatedAgency);

                // NOTE: This is a sanity-check to ensure we don't bombard clients with
                // alerts if there's an issue with database persistence.
                if (alertsPersisted) {
                    Logger.debug("New Agency Alerts persisted. Sending to subscribers.");
                    mPushMessageManager.dispatchAlerts(agencyModifications);
                }

            } else {
                Logger.info(String.format("No alerts found in updated agency: %d.", updatedAgency.id));
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
    private AgencyModifications getUpdatedRoutesAlerts(@Nonnull Agency agency) {
        AgencyModifications modifiedRouteAlerts = new AgencyModifications(agency.id);

        // Get all new routes, and the current routes that exist for the agency.
        List<Route> freshRoutes = CommuteAlertHelper.copyRoutes(agency.routes);
        List<Route> existingRoutes = CommuteAlertHelper.copyRoutes(mAgencyService.getAgencyRoutes(agency.id));

        // If there are no existing alerts saved, mark all fetched alerts as new.
        if (existingRoutes.isEmpty()) {
            Logger.info(String.format("Existing routes for agency %s missing. Marked all as updated.", agency.name));
            modifiedRouteAlerts.addUpdatedRoute(agency.routes);
            return modifiedRouteAlerts;
        }

        // If there are no fetched alerts at all, mark all existing alerts as stale.
        if (freshRoutes == null || freshRoutes.isEmpty()) {
            Logger.info(String.format("New routes for agency %s missing. Marked all as stale.", agency.name));
            modifiedRouteAlerts.addStaleRoute(agency.routes);
            return modifiedRouteAlerts;
        }

        // Iterate through the new routes (outer), and existing routes (inner).
        for (Route freshRoute : freshRoutes) {
            for (Route existingRoute : existingRoutes) {

                // Check existing and new Routes are the same.
                if (freshRoute.routeId.equals(existingRoute.routeId)) {

                    // If there are no new alerts but there are existing alerts.
                    boolean missingFreshRouteAlerts = ((freshRoute.alerts == null || freshRoute.alerts.isEmpty()) &&
                            (existingRoute.alerts != null && !existingRoute.alerts.isEmpty()));

                    // If there are no existing alerts but there are new alerts.
                    boolean missingExistingRouteAlerts = ((existingRoute.alerts == null || existingRoute.alerts.isEmpty()) &&
                            (existingRoute.alerts != null && !existingRoute.alerts.isEmpty()));

                    if (missingFreshRouteAlerts || missingExistingRouteAlerts) {
                        modifiedRouteAlerts.addUpdatedRoute(freshRoute);
                    }

                    Set<AlertType> updatedAlertTypes = new HashSet<>();
                    List<Alert> updatedAlerts = new ArrayList<>();

                    /*
                     * Add a route alert as updated if:
                     * 1) The fresh alert does not exist in the new route alert list.
                     */
                    if (freshRoute.alerts != null) {
                        for (Alert freshAlert : freshRoute.alerts) {

                            // Record the new alert, and alertType.
                            if (!CommuteAlertHelper.isAlertEmpty(freshAlert) &&
                                    !existingRoute.alerts.contains(freshAlert)) {

                                Logger.info(String.format("%1$s alert for existing route %2$s has updated.",
                                        freshAlert.type, existingRoute.routeId));

                                updatedAlertTypes.add(freshAlert.type);
                                updatedAlerts.add(freshAlert);
                            }
                        }
                    }

                    /*
                     * Add a route alert as stale if:
                     * 1) An existing alert for the route no longer exists in the fresh route.
                     * 2) The alertType was not previously recorded as being updated.
                     */
                    List<Route> staleRoutes = new ArrayList<>();
                    if (existingRoute.alerts != null) {

                        for (Alert existingAlert : existingRoute.alerts) {
                            if (!freshRoute.alerts.contains(existingAlert) &&
                                    !updatedAlertTypes.contains(existingAlert.type)) {

                                Logger.info(String.format("%1$s alert for fresh route %2$s went stale.",
                                        existingAlert.type, existingRoute.routeId));

                                freshRoute.alerts = new ArrayList<>();
                                staleRoutes.add(freshRoute);
                                break;
                            }
                        }
                    }

                    // Add only the new updated fresh alerts into the freshRoute.
                    if (!updatedAlerts.isEmpty()) {
                        freshRoute.alerts = updatedAlerts;
                        modifiedRouteAlerts.addUpdatedRoute(freshRoute);
                    }

                    // Set the stale routes.
                    modifiedRouteAlerts.addStaleRoute(staleRoutes);

                    // There was a route match so skip the inner loop.
                    break;
                }
            }
        }
        return modifiedRouteAlerts;
    }
}