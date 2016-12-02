package models;

import models.alerts.Alert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of new, uddated, or removed alerts for an
 * {@link models.alerts.Agency}
 */
public class AgencyAlertModifications {
    private int mAgencyId;
    private List<Alert> mUpdatedAlerts = new ArrayList<>();
    private List<Alert> mStaleAlerts = new ArrayList<>();

    /**
     * Create a alert modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public AgencyAlertModifications(int agencyId) {
        mAgencyId = agencyId;
    }

    /**
     * Check to see if there are modified alerts for an alert / agency.
     * These include alerts which are  new, updated, or stale.
     *
     * @return true if the agency has updated alerts within this model.
     */
    public boolean hasModifiedAlerts() {
        return !mStaleAlerts.isEmpty() || !mUpdatedAlerts.isEmpty();
    }

    /**
     * Add a list of new or updated alerts.
     *
     * @param alerts The alerts to set as updated or new.
     */
    public void addUpdatedAlerts(List<Alert> alerts) {
        if (alerts != null) {
            for (Alert alert : alerts) {
                addUpdatedAlert(alert);
            }
        }
    }

    /**
     * Add a new, or updated alert.
     *
     * @param alert The alert to flag as updated or new.
     */
    public void addUpdatedAlert(@Nonnull Alert alert) {
        if (alert.route == null || alert.route.routeId == null || alert.route.routeId.isEmpty()) {
            throw new RuntimeException("Updated alert must include a route parent.");
        }

        mUpdatedAlerts.add(alert);
    }

    /**
     * Add a list of stale or purged alerts.
     *
     * @param alerts The alerts to set as updated or stale.
     */
    public void addStaleAlerts(@Nonnull List<Alert> alerts) {
        for (Alert alert : alerts) {
            addStaleAlert(alert);
        }
    }

    /**
     * Add a alert which contain stale, purged alerts.
     *
     * @param alert The alerts to add as stale.
     */
    public void addStaleAlert(@Nonnull Alert alert) {
        mStaleAlerts.add(alert);
    }

    /**
     * Get a list of all new or updated Agency {@link Alert}s.
     *
     * @return list of updated Agency alerts.
     */
    public List<Alert> getUpdatedAlerts() {
        return mUpdatedAlerts;
    }

    /**
     * Get a list of all stale Agency {@link Alert}s.
     *
     * @return list of all stale alerts.
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
