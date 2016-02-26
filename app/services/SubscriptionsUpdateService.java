package services;

import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.app.ModifiedAlerts;

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
public class SubscriptionsUpdateService {

    public void agencyUpdated(@Nonnull Agency updatedAgency) {
        ModifiedAlerts modifiedAlerts = getUpdatedAlerts(updatedAgency);
        if (modifiedAlerts.hasModifiedAlerts()) {

        }

    }

    /**
     * Creates a list of new and removed alerts for a given agency bundle.
     *
     * @param updatedAgency the agency which is to be updated.
     * @return A list of removed and added alerts for that agency.
     */
    private ModifiedAlerts getUpdatedAlerts(@Nonnull Agency updatedAgency) {
        ModifiedAlerts modifiedAlerts = new ModifiedAlerts(updatedAgency.agencyId);

        AgencyDatabaseService agencyDatabaseService = AgencyDatabaseService.getInstance();
        List<Route> oldRoutes = agencyDatabaseService.getRoutes(updatedAgency.agencyId);

        if (oldRoutes != null) {
            Collections.sort(oldRoutes);

            List<Route> newRoutes = updatedAgency.routes;
            Collections.sort(newRoutes);

            // Loop through the saved routes.
            for (Route oldRoute : oldRoutes) {

                // Inner loop through the new routes.
                for (Route newRoute : newRoutes) {

                    // Check that the routeId matches and that there's a difference in alerts.
                    if (newRoute.routeId.equals(oldRoute.routeId) &&
                            !newRoute.alerts.equals(oldRoute.alerts)) {

                        List<Alert> oldAlerts = oldRoute.alerts;
                        List<Alert> newAlerts = newRoute.alerts;

                        Collections.sort(oldAlerts);
                        Collections.sort(newAlerts);

                        // Either add the new alerts if it is new, or remove the old
                        // alert if it does not exist in the new alerts anymore.
                        for (Alert oldAlert : oldAlerts) {

                            // Loop through the new list of alerts for the route.
                            for (Alert newAlert : newAlerts) {

                                if (!oldAlerts.contains(newAlert)) {
                                    newAlert.routeId = newRoute.routeId;
                                    modifiedAlerts.mAddedRouteAlerts.add(newAlert);
                                }
                            }

                            if (!newAlerts.contains(oldAlert)) {
                                oldAlert.routeId = oldRoute.routeId;
                                modifiedAlerts.mRemovedRouteAlerts.add(oldAlert);
                            }
                        }

                        // We found the matching route. Break out of the inner loop.
                        break;
                    }
                }
            }
        }

        return modifiedAlerts;
    }
}
