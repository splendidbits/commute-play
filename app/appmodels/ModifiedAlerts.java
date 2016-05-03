package appmodels;

import models.alerts.Alert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of new, uddated, or removed alerts for an
 * {@link models.alerts.Agency}
 */
public class ModifiedAlerts {
    private int mAgencyId;
    private List<Alert> mUpdatedAlerts = new ArrayList<>();
    private List<Alert> mStaleAlerts = new ArrayList<>();

    @SuppressWarnings("unused")
    private ModifiedAlerts() {
    }

    /**
     * Create a alert modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public ModifiedAlerts(int agencyId) {
        mAgencyId = agencyId;
    }

    /**
     * Check to see if there are modified alerts for an route / agency.
     * These include alerts which are  new, updated, or stale.
     *
     * @return true if the agency has updated alerts within this model.
     */
    public boolean hasModifiedAlerts() {
        return !mUpdatedAlerts.isEmpty() || !mStaleAlerts.isEmpty();
    }

    /**
     * Add a list of route alerts as new or updated. It will not be added if it has already
     * been added as stale.
     *
     * @param alerts The alert to flag as updated or new.
     */
    public void addUpdatedAlerts(@Nonnull List<Alert> alerts) {
        // Do not add new alert if it exists in the stale collection.
        for (Alert alert : alerts) {
            if (!mStaleAlerts.contains(alert)) {
                mUpdatedAlerts.add(alert);
            }
        }
    }

    /**
     * Add a route alert as new or updated. It will not be added if it has already
     * been added as stale.
     *
     * @param alert The alert to flag as updated or new.
     */
    public void addUpdatedAlert(@Nonnull Alert alert) {
        // Do not add new alert if it exists in the stale collection.
        if (!mStaleAlerts.contains(alert)) {
            mUpdatedAlerts.add(alert);
        }
    }

    /**
     * Add a list of route alerts as stale. It will not be added if it has already
     * been added as new or updated.
     *
     * @param alerts The alerts to flag as stale or missing.
     */
    public void addStaleAlerts(@Nonnull List<Alert> alerts) {
        // Do not add stale alert if it exists in the updated collection.
        for (Alert alert : alerts) {
            if (!mUpdatedAlerts.contains(alert)) {
                mStaleAlerts.add(alert);
            }
        }
    }

    /**
     * Add a route alert as stale. It will not be added if it has already
     * been added as new or updated.
     *
     * @param alert The alert to flag as stale or missing.
     */
    public void addStaleAlert(@Nonnull Alert alert) {
        // Do not add stale alert if it exists in the updated collection.
        if (!mUpdatedAlerts.contains(alert)) {
            mStaleAlerts.add(alert);
        }
    }

    /**
     * Get a list of all the agency alerts than are new or updated.
     *
     * @return list of updated alerts.
     */
    public List<Alert> getUpdatedAlerts() {
        return mUpdatedAlerts;
    }

    /**
     * Get a list of all the agency alerts than are old and stale.
     *
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
