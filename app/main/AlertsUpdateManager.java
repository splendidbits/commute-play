package main;

import dispatcher.processors.PushMessageService;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import appmodels.ModifiedAlerts;
import services.AgencyService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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

    private AgencyService mAgencyService;
    private PushMessageService mPushMessageService;
    private Log mLog;

    @Inject
    public AlertsUpdateManager(Log log, AgencyService agencyService, PushMessageService pushMessageService) {
        mAgencyService = agencyService;
        mLog = log;
        mPushMessageService = pushMessageService;
    }

    /**
     * Starts of a chain of things when an agency is passed into this method:
     * <p>
     * 1) Builds a list of added and removed alerts for the agency, since the last time.
     * 2) Saves the modified routes in the database.
     * 3) Passes on the alert information to subscribed GCM clients.
     * 4) Modifies any registration information based on the response from Google.
     *
     * @param updatedAgency The agency which has been updated.
     */
    public void saveAndNotifyAgencySubscribers(@Nonnull Agency updatedAgency) {
        List<Route> agencyRoutes = updatedAgency.routes;
        if (agencyRoutes != null && !agencyRoutes.isEmpty()) {
            Collections.sort(agencyRoutes);

            // Pass the Alert differences on to the GCM Pre-processor.
            ModifiedAlerts modifiedAlerts = getUpdatedRoutes(updatedAgency);
            if (modifiedAlerts.hasModifiedAlerts()) {

                mLog.d(TAG, "Found new alerts in agency routes.");
                mPushMessageService.notifyAlertSubscribers(modifiedAlerts);

                // Save the agency in the datastore.
                mLog.d(TAG, "Saving new or updated agency data.");
                mAgencyService.saveAgencyAlerts(updatedAgency);
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
    private ModifiedAlerts getUpdatedRoutes(@Nonnull Agency updatedAgency) {
        ModifiedAlerts modifiedAlerts = new ModifiedAlerts(updatedAgency.id);

        // Get all the current routes that exist for the agency.
        List<Route> existingRouteAlerts = mAgencyService.getRouteAlerts(updatedAgency.id);

        // Get all the fresh routes that have been parsed.
        List<Route> freshRouteAlerts = updatedAgency.routes;

        // If there are no existing alerts saved, mark all fetched alerts as new.
        if (existingRouteAlerts == null || existingRouteAlerts.isEmpty()) {
            for (Route freshRoute : freshRouteAlerts) {
                for (Alert freshAlert : freshRoute.alerts) {
                    modifiedAlerts.addUpdatedRouteAlert(freshAlert);
                }
            }
            return modifiedAlerts;
        }

        // If there are no fetched alerts at all, mark all existing alerts as stale.
        if (freshRouteAlerts == null || freshRouteAlerts.isEmpty()) {
            for (Route existingRoute : existingRouteAlerts) {
                for (Alert existingAlert : existingRoute.alerts) {
                    modifiedAlerts.addStaleRouteAlert(existingAlert);
                }
            }
            return modifiedAlerts;
        }

        // Iterate through the fresh routes that have been parsed.
        for (Route freshRoute : freshRouteAlerts) {

            // Iterate through the existing routes.
            for (Route existingRoute : existingRouteAlerts) {
                if (freshRoute.routeId.equals(existingRoute.routeId)) {

                    // Check if the fresh alert is new (updated properties).
                    for (Alert freshAlert : freshRoute.alerts) {
                        if (!existingRoute.alerts.contains(freshAlert)) {
                            modifiedAlerts.addUpdatedRouteAlert(freshAlert);
                        }
                    }

                    // Add the alert as stale if it no longer exists.
                    for (Alert existingAlert : existingRoute.alerts) {
                        if (!freshRoute.alerts.contains(existingAlert)) {
                            modifiedAlerts.addStaleRouteAlert(existingAlert);
                        }
                    }

                    // Skip all the inner route iterations.
                    break;
                }
            }
        }

        return modifiedAlerts;
    }
}
