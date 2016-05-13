package services;

import agency.AgencyModifications;
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

    private static final String TAG = AlertsUpdateManager.class.getSimpleName();

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
    public void saveAndNotifyAgencySubscribers(@Nonnull Agency updatedAgency) {
        AgencyModifications agencyModifications = getUpdatedRoutesAlerts(updatedAgency);
        if (agencyModifications.hasModifiedRoutes()) {

            // Save the agency in the datastore.
            Logger.debug("Saving new or updated agency data.");
            boolean alertsPersisted = mAgencyService.saveAgencyAlerts(updatedAgency);

            // NOTE: This is a sanity-check to ensure we don't bombard clients with
            // alerts if there's an issue with database persistence.
            if (alertsPersisted) {
                Logger.debug("New Agency Alerts persisted. Sending to subscribers.");
                mPushMessageManager.dispatchAlerts(agencyModifications);
            }
        }
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param updatedAgency the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    @Nonnull
    private AgencyModifications getUpdatedRoutesAlerts(@Nonnull Agency updatedAgency) {
        AgencyModifications modifiedRouteAlerts = new AgencyModifications(updatedAgency.id);

        // Get all new routes, and the current routes that exist for the agency.
        List<Route> freshRoutes = CommuteAlertHelper.copyRoutes(updatedAgency.routes);
        List<Route> existingRoutes = CommuteAlertHelper.copyRoutes(mAgencyService.getAgencyRoutes(updatedAgency.id));

        // If there are no existing alerts saved, mark all fetched alerts as new.
        if (existingRoutes == null || existingRoutes.isEmpty()) {
            modifiedRouteAlerts.addUpdatedRoute(updatedAgency.routes);
            return modifiedRouteAlerts;
        }

        // If there are no fetched alerts at all, mark all existing alerts as stale.
        if (freshRoutes == null || freshRoutes.isEmpty()) {
            modifiedRouteAlerts.addUpdatedRoute(updatedAgency.routes);
            return modifiedRouteAlerts;
        }

        for (Route freshRoute : freshRoutes) {
            for (Route existingRoute : existingRoutes) {

                // Existing and new Routes are the same.
                if (freshRoute.routeId.equals(existingRoute.routeId)) {

                    // If there are no new alerts and there are existing alerts.
                    if ((freshRoute.alerts == null || freshRoute.alerts.isEmpty()) &&
                            (existingRoute.alerts != null && !existingRoute.alerts.isEmpty())) {
                        modifiedRouteAlerts.addUpdatedRoute(freshRoute);
                        break;
                    }

                    // If there are no existing alerts and there are new alerts.
                    if ((existingRoute.alerts == null || existingRoute.alerts.isEmpty()) &&
                            (existingRoute.alerts != null && !existingRoute.alerts.isEmpty())) {
                        modifiedRouteAlerts.addUpdatedRoute(freshRoute);
                        break;
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
