package services;

import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.app.ModifiedAlerts;

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
public class AgencyUpdateService {

    public void agencyUpdated(@Nonnull Agency updatedAgency) {
        ModifiedAlerts modifiedAlerts = getUpdatedRoutes(updatedAgency);
        //if (modifiedAlerts.hasModifiedAlerts()) {
            Collections.sort(updatedAgency.routes);
            AgencyDatabaseService alertsService = AgencyDatabaseService.getInstance();
            alertsService.saveAgencyAlerts(updatedAgency);
        //}
    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param updatedAgency the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
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
            modifiedAlerts.mAddedRouteAlerts = freshAlerts;
            return modifiedAlerts;
        }

        // Check if the fresh alert is new (updated properties).
        for (Alert freshAlert : freshAlerts) {
            if (!currentAlerts.contains(freshAlert)) {

            }
        }

        return modifiedAlerts;
    }
}
