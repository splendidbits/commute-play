package models;

import models.alerts.Alert;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of new, updated, or removed (stale) alerts for an {@link models.alerts.Agency}
 */
public class AlertModifications {
    private int mAgencyId;
    private List<Alert> mUpdatedAlerts = new ArrayList<>();
    private List<Alert> mStaleAlerts = new ArrayList<>();

    /**
     * Create a modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public AlertModifications(int agencyId) {
        mAgencyId = agencyId;
    }

    /**
     * Check to see if there are modified alerts (either new / stale).
     *
     * @return true if the agency has updated alerts since the previous agency set.
     */
    public boolean hasChangedAlerts() {
        return !mUpdatedAlerts.isEmpty() || !mStaleAlerts.isEmpty();
    }

    /**
     * Add a new, or updated alert.
     *
     * @param alert The alert to flag as updated or new.
     */
    public void addUpdatedAlert(@Nonnull Alert alert) {
        if (!StringUtils.isBlank(alert.route.routeId)) {
            mUpdatedAlerts.add(alert);
        }
    }

    /**
     * Add an alert that is stale (used to exist but no longer does.
     *
     * @param alert The alert which is deemed stale.
     */
    public void addStaleAlert(@Nonnull Alert alert) {
        if (!StringUtils.isBlank(alert.route.routeId)) {
            if (!mUpdatedAlerts.contains(alert)) {
                mStaleAlerts.add(alert);
            }
        }
    }

    /**
     * Get a list of all new or updated Agency {@link Alert}'s.
     *
     * @return list of updated alerts.
     */
    public List<Alert> getUpdatedAlerts() {
        return mUpdatedAlerts;
    }

    /**
     * Get all alerts, which should be considered stale)
     * @return list of stale alerts.
     */
    public List<Alert> getStaleAlerts() {
        return mStaleAlerts;
    }

    /**
     * Get the agencyId for these alert modifications.
     *
     * @return Agency identifier.
     */
    public int getAgencyId() {
        return mAgencyId;
    }
}
