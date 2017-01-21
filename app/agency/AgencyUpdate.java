package agency;

import helpers.AlertHelper;
import models.AgencyAlertModifications;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import services.AgencyManager;
import services.PushMessageManager;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * Base class that facilitates downloading alerts from an agency's server and sending them to the
 * dispatch processes.
 * <p>
 * Download current alerts.
 * 1: Download agency alerts.
 * 2: Bundle into standard format.
 * <p>
 * Send to GCM processor
 * <p>
 * 2.5: Go through each Route > Alert bundle and find any differences
 * 3: Collect the new alerts
 * 4: Persist new data
 * 5: Get list of subscriptions for route
 * 6: send data in batches of 1000 to google.
 */
public abstract class AgencyUpdate {
    protected static final int AGENCY_DOWNLOAD_TIMEOUT_MS = 1000 * 60;
    private PushMessageManager mPushMessageManager;
    private AgencyManager mAgencyManager;

    protected AgencyUpdate(@Nonnull AgencyManager agencyManager, @Nonnull PushMessageManager pushMessageManager) {
        mAgencyManager = agencyManager;
        mPushMessageManager = pushMessageManager;
    }

    /**
     * Method called when the agency data should be downloaded and sorted into an {@link Agency}
     * model data.
     * When this is complete, call processAgencyUpdate() and supply the Agency data.
     */
    public abstract void startAgencyUpdate();

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
    protected void processAgencyUpdate(Agency updatedAgency) {
        if (updatedAgency != null) {
            // Comment to disable load test.
            // createLoadTestUpdates(updatedAgency);

            // Add the parent route back into each alert model.
            fillAlertsWithRoutes(updatedAgency);

            // Parse html and fix text formatting inconsistencies.
            AlertHelper.parseHtml(updatedAgency);

            // Diff the new and existing agency data and form a modifications model.
            Agency existingAgency = mAgencyManager.getSavedAgency(updatedAgency.id);
            AgencyAlertModifications agencyModifications = AlertHelper.getAgencyModifications(existingAgency, updatedAgency);

            if (agencyModifications != null && agencyModifications.hasModifiedAlerts()) {
                // Save the agency in the datastore.
                Logger.debug("Saving new or updated agency data.");

                // Don't send notifications to clients with alerts if there's an issue with database persistence.
                boolean alertsPersisted = mAgencyManager.saveAgency(updatedAgency);
                if (alertsPersisted) {
                    Logger.debug("New Agency Alerts persisted. Sending to subscribers.");
                    mPushMessageManager.dispatchAlerts(agencyModifications);
                }

            } else {
                Logger.info(String.format("No changed alerts found for agency: %s.", updatedAgency.name));
            }
        }
    }

    /**
     * Modify the data in a series of route alerts to test things.
     *
     * @param agency agency bundle.
     */
    private void createLoadTestUpdates(@Nonnull Agency agency) {
        if (agency.routes != null) {
            Alert previousAlert = null;

            Collections.shuffle(agency.routes);
            for (Route route : agency.routes) {

                if (route.alerts != null) {
                    Collections.shuffle(route.alerts);
                    for (Alert alert : route.alerts) {
                        alert.messageTitle = previousAlert != null
                                ? previousAlert.messageTitle
                                : alert.messageTitle;

                        alert.messageSubtitle = previousAlert != null
                                ? previousAlert.messageSubtitle
                                : alert.messageSubtitle;

                        alert.messageBody = previousAlert != null
                                ? previousAlert.messageBody
                                : alert.messageBody;

                        previousAlert = alert;
                    }
                }
            }
        }
    }

    /**
     * Iterate through all alerts in all routes, and add the route parent model to each
     * alert -> route relation.
     *
     * @param agency The specified agency to fill alerts with routes.
     */
    private void fillAlertsWithRoutes(@Nonnull Agency agency) {
        // Add the route model into each alert.
        if (agency.routes != null) {
            for (Route route : agency.routes) {
                if (route.alerts != null) {
                    for (Alert alert : route.alerts) {
                        if (alert.route == null) {
                            alert.route = route;
                        }
                    }
                }
            }
        }
    }
}