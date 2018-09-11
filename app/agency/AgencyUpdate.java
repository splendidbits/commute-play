package agency;

import helpers.AlertHelper;
import models.AlertModifications;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import play.Logger;
import services.AgencyManager;
import services.PushMessageManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

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
        if (updatedAgency != null && updatedAgency.getRoutes() != null) {
            // Parse html and fix text formatting inconsistencies.
            AlertHelper.parseHtml(updatedAgency);

            // Get existing alerts for the agency.
            Agency existingAgency = mAgencyManager.getSavedAgency(updatedAgency.getId(), false);
            if (existingAgency == null) {
                Logger.info(String.format("No existing agency found for %s. Saving but not dispatching.", updatedAgency.getName()));
                mAgencyManager.saveAgency(updatedAgency);
                return;
            }

            // Diff the new and existing agency data and form a modifications model.
            AlertModifications modifications = AlertHelper.getAgencyModifications(existingAgency, updatedAgency);

            // Log some shit.
            int updatedMessagesCount = 0;
            for (Map.Entry<Route, List<Alert>> entry : modifications.getUpdatedAlerts().entrySet()) {
                updatedMessagesCount += entry.getValue().size();
            }

            int staleMessagesCount = 0;
            for (Map.Entry<Route, List<Alert>> entry : modifications.getStaleAlerts().entrySet()) {
                staleMessagesCount += entry.getValue().size();
            }
            int totalMessagesCount = updatedMessagesCount + staleMessagesCount;

            Logger.info(String.format("%s messages found for %s.", totalMessagesCount > 0 ? "* Updated" : "No updated", updatedAgency.getName()));
            Logger.info(String.format("[%d] new messages.", updatedMessagesCount));
            Logger.info(String.format("[%d] stale messages.", staleMessagesCount));

            if (modifications.hasChangedAlerts()) {
                mAgencyManager.saveAgency(updatedAgency);

                Logger.info(String.format("Updated %s Agency Alerts persisted. Sending to subscribers.", updatedAgency.getName()));
                mPushMessageManager.dispatchAlerts(modifications);

            } else {
                mAgencyManager.cacheAgency(updatedAgency);
            }
        }
    }
}