package main;

import agency.AgencyModifications;
import helpers.PushMessageManager;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.AgencyDao;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
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

    private AgencyDao mAgencyService;
    private PushMessageManager mPushMessageManager;
    private Log mLog;

    @Inject
    public AlertsUpdateManager(Log log, AgencyDao agencyService, PushMessageManager pushMessageManager) {
        mAgencyService = agencyService;
        mLog = log;
        mPushMessageManager = pushMessageManager;
    }

    /**
     * Starts of a chain of things when an agency is passed into this method:
     * <p>
     * 1) Builds a list of added and removed alerts for the agency, since the last time.
     * 2) Saves the modified routes in the database.
     * 3) Passes on the alert information to subscribed GCM clients.
     * 4) Modifies any device information based on the response from Google.
     *
     * @param updatedAgency The agency which has been updated.
     */
    public void saveAndNotifyAgencySubscribers(@Nonnull Agency updatedAgency) {
        List<Route> agencyRoutes = updatedAgency.routes;
        if (!agencyRoutes.isEmpty()) {

            // Pass the Alert differences on to the GCM Pre-processor.
            AgencyModifications modifiedAlerts = getUpdatedRoutes(updatedAgency);
            if (modifiedAlerts.hasModifiedRoutes()) {

                // Save the agency in the datastore.
                mLog.d(TAG, "Saving new or updated agency data.");
                boolean alertsPersisted = mAgencyService.saveAgencyAlerts(updatedAgency);

                // NOTE: This is a sanity-check to ensure we don't bombard clients with
                // alerts if there's an issue with database persistence.
                if (alertsPersisted) {
                    mLog.d(TAG, "New Agency Alerts persisted. Sending to subscribers.");
                    mPushMessageManager.dispatchAlerts(modifiedAlerts);
                }
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
    private AgencyModifications getUpdatedRoutes(@Nonnull Agency updatedAgency) {
        AgencyModifications modifiedRouteAlerts = new AgencyModifications(updatedAgency.id);

        // Get all new routes, and the current routes that exist for the agency.
        List<Route> freshRoutes = updatedAgency.routes;
        List<Route> existingRoutes = mAgencyService.getRouteAlerts(updatedAgency.id);

        // If there are no existing alerts saved, mark all fetched alerts as new.
        if (existingRoutes == null || existingRoutes.isEmpty()) {
            modifiedRouteAlerts.addUpdatedRouteAlert(updatedAgency.routes);
            return modifiedRouteAlerts;
        }

        // If there are no fetched alerts at all, mark all existing alerts as stale.
        if (freshRoutes == null || freshRoutes.isEmpty()) {
            modifiedRouteAlerts.addStaleRouteAlert(updatedAgency.routes);
            return modifiedRouteAlerts;
        }

        List<Route> updatedRoutes = new ArrayList<>();
        List<Route> staleRoutes = new ArrayList<>();
        for (Route freshRoute : freshRoutes) {

            // Iterate through the existing persisted routes.
            for (Route existingRoute : existingRoutes) {

                // Routes are the same.
                if (freshRoute.routeId.equals(existingRoute.routeId) &&
                        freshRoute.alerts != null &&
                        existingRoute.alerts != null) {

                    // Check if the fresh alert is new (updated properties).
                    List<Alert> updatedRouteAlerts = new ArrayList<>();
                    for (Alert freshAlert : freshRoute.alerts) {
                        if (!existingRoute.alerts.contains(freshAlert)) {
                            updatedRouteAlerts.add(freshAlert);
                        }
                    }

                    // Add the alert as stale if it no longer exists.
                    List<Alert> staleRouteAlerts = new ArrayList<>();
                    for (Alert existingAlert : existingRoute.alerts) {
                        if (!freshRoute.alerts.contains(existingAlert)) {
                            staleRouteAlerts.add(existingAlert);
                        }
                    }

                    if (!staleRouteAlerts.isEmpty()) {
                        existingRoute.alerts = staleRouteAlerts;
                        staleRoutes.add(existingRoute);
                    }

                    if (!updatedRouteAlerts.isEmpty()) {
                        existingRoute.alerts = updatedRouteAlerts;
                        updatedRoutes.add(existingRoute);
                    }

                    // As we found a route match, skip out of the inner existing route.
                    break;
                }
            }
        }

        modifiedRouteAlerts.addUpdatedRouteAlert(updatedRoutes);
        modifiedRouteAlerts.addStaleRouteAlert(staleRoutes);

        return modifiedRouteAlerts;
    }
}
