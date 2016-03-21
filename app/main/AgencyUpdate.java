package main;

import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.app.ModifiedAlerts;
import services.AgencyServiceDao;
import services.gcm.GcmAlertProcessor;

import javax.annotation.Nonnull;
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
public class AgencyUpdate {

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
            if (modifiedAlerts.hasModifiedAlerts()){
                GcmAlertProcessor preprocessor = new GcmAlertProcessor();
                preprocessor.notifyAlertSubscribers(modifiedAlerts);

                // Save the agency in the datastore.
                AgencyServiceDao alertsService = AgencyServiceDao.getInstance();
                //alertsService.saveAgencyAlerts(updatedAgency);
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
        ModifiedAlerts modifiedAlerts = new ModifiedAlerts(updatedAgency.agencyId);
        AgencyServiceDao agencyServiceDao = AgencyServiceDao.getInstance();

        // Get all the current routes that exist for the agency.
        List<Route> currentRouteAlerts = agencyServiceDao.getRouteAlerts(updatedAgency.agencyId);

        // Get all the fresh routes that have been parsed.
        List<Route> freshRouteAlerts = updatedAgency.routes;

        // If there are no current alerts, they are all new.
        if (currentRouteAlerts == null || currentRouteAlerts.isEmpty()) {
            for (Route freshRoute : freshRouteAlerts) {
                for (Alert freshAlert : freshRoute.alerts) {
                    modifiedAlerts.addUpdatedRouteAlert(freshAlert);
                }
            }
            return modifiedAlerts;
        }

        // Iterate through the fresh routes that have been parsed.
        for (Route freshRoute : freshRouteAlerts) {

            // Iterate through the existing routes.
            for (Route currentRoute : currentRouteAlerts) {

                // If both routes are the same, then check the alerts.
                if (currentRoute.routeId.equals(freshRoute.routeId)) {

                    // Check if the fresh alert is new (updated properties).
                    for (Alert freshAlert : freshRoute.alerts) {
                        if (!currentRoute.alerts.contains(freshAlert)) {
                            modifiedAlerts.addUpdatedRouteAlert(freshAlert);
                        }
                    }

                    // Check if a current alert is stale.
                    for (Alert currentAlert : currentRoute.alerts) {
                        if (!currentRoute.alerts.contains(currentAlert)) {
                            modifiedAlerts.addStaleRouteAlert(currentAlert);
                        }
                    }
                }
            }
        }

        return modifiedAlerts;
    }
}
