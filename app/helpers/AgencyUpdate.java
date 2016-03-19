package helpers;

import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.app.ModifiedAlerts;
import services.AgencyDatabaseService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
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

            // Save the agency in the datastore.
            AgencyDatabaseService alertsService = AgencyDatabaseService.getInstance();
            alertsService.saveAgencyAlerts(updatedAgency);

            // Pass the Alert differences on to the GCM Pre-processor.
            ModifiedAlerts modifiedAlerts = getUpdatedRoutes(updatedAgency);
            if (modifiedAlerts.hasModifiedAlerts()){
                GcmAlertProcessor preprocessor = new GcmAlertProcessor();
                preprocessor.notifyAlertSubscribers(modifiedAlerts);
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

        // Grab all existing alerts for the agency.
        AgencyDatabaseService agencyDatabaseService = AgencyDatabaseService.getInstance();
        List<Alert> currentAlerts = agencyDatabaseService.getRouteAlerts(updatedAgency.agencyId);

        // Loop through each new route / alert and add to a temporary array.
        List<Alert> freshAlerts = new ArrayList<>();
        for (Route route : updatedAgency.routes) {
            if (route.alerts != null) {
                for (Alert freshAlert : route.alerts) {
                    freshAlerts.add(freshAlert);
                }
            }
        }

        // If there are no current alerts, they are all new.
        if (currentAlerts == null || currentAlerts.isEmpty()) {
            for (Alert freshAlert : freshAlerts) {
                modifiedAlerts.addUpdatedRouteAlert(freshAlert, freshAlert.route);
            }
            return modifiedAlerts;
        }

        // Check if the fresh alert is new (updated properties).
        for (Alert freshAlert : freshAlerts) {
            if (!currentAlerts.contains(freshAlert)) {
                modifiedAlerts.addUpdatedRouteAlert(freshAlert, freshAlert.route);
            }
        }

        // Check if a current alert is stale.
        for (Alert currentAlert : currentAlerts) {
            if (!freshAlerts.contains(currentAlert)) {
                modifiedAlerts.addStaleRouteAlert(currentAlert, currentAlert.route);
            }
        }

        return modifiedAlerts;
    }
}
