package agency;

import dao.AgencyDao;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.libs.ws.WSClient;
import services.AlertsUpdateManager;
import services.PushMessageManager;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;

abstract class AgencyUpdate {
    int AGENCY_DOWNLOAD_TIMEOUT_MS = 1000 * 60;
    private AgencyDao mAgencyDao;
    private PushMessageManager mPushMessageManager;
    private AlertsUpdateManager mAlertsUpdateManager;
    protected WSClient mWsClient;

    protected AgencyUpdate(@Nonnull WSClient wsClient, @Nonnull AlertsUpdateManager alertsUpdateManager,
                           @Nonnull AgencyDao agencyDao, @Nonnull PushMessageManager pushMessageManager) {
        mWsClient = wsClient;
        mAlertsUpdateManager = alertsUpdateManager;
        mAgencyDao = agencyDao;
        mPushMessageManager = pushMessageManager;
    }

    private AgencyUpdate() {
    }

    /**
     * Method called when the agency data should be downloaded and sorted into an {@link Agency}
     * model data.
     * When this is complete, call processAgencyUpdate() and supply the Agency data.
     */
    public void startAgencyUpdate() {
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
    public void processAgencyUpdate(@Nonnull Agency updatedAgency) {
        // Add the parent route back into each alert model.
        fillAlertsWithRoutes(updatedAgency);

        // Diff the new and existing agency data and form a modifications model.
        AgencyAlertModifications agencyAlertModifications = mAlertsUpdateManager.getUpdatedRoutesAlerts(updatedAgency);

        if (agencyAlertModifications.hasModifiedAlerts()) {
            // Save the agency in the datastore.
            Logger.debug("Saving new or updated agency data.");
            boolean alertsPersisted = mAgencyDao.saveAgency(updatedAgency);

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
                        alert.route = route;
                    }
                }
            }
        }

    }
}