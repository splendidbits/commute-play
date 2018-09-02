package models;

import models.alerts.Alert;
import models.alerts.Route;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains a list of new, updated, or removed (stale) alerts for an {@link models.alerts.Agency}
 */
public class AlertModifications {
    private String mAgencyId;
    private Map<Route, List<Alert>> mUpdatedAlerts = new HashMap<>();
    private Map<Route, List<Alert>> mStaleAlerts = new HashMap<>();

    /**
     * Create a modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public AlertModifications(String agencyId) {
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
    public void addUpdatedAlert(@Nonnull Route route, @Nonnull Alert alert) {
        List<Alert> alerts = new ArrayList<>();

        for (Map.Entry<Route, List<Alert>> updatedEntry : mUpdatedAlerts.entrySet()) {
            Route updatedRoute = updatedEntry.getKey();
            List<Alert> updatedAlerts = updatedEntry.getValue();

            if (updatedRoute.getRouteId().toLowerCase().equals(route.getRouteId().toLowerCase())) {
                alerts = updatedAlerts;
            }
        }

        alerts.add(alert);
        mUpdatedAlerts.put(route, alerts);
    }

    /**
     * Add an alert that is stale (used to exist but no longer does.
     *
     * @param alert The alert which is deemed stale.
     */
    public void addStaleAlert(@Nonnull Route route, @Nonnull Alert alert) {
        List<Alert> alerts = new ArrayList<>();

        for (Map.Entry<Route, List<Alert>> updatedEntry : mStaleAlerts.entrySet()) {
            Route updatedRoute = updatedEntry.getKey();
            List<Alert> updatedAlerts = updatedEntry.getValue();

            if (updatedRoute.getRouteId().toLowerCase().equals(route.getRouteId().toLowerCase())) {
                alerts = updatedAlerts;
            }
        }

        alerts.add(alert);
        mStaleAlerts.put(route, alerts);
    }

    /**
     * Get a list of all new or updated Agency {@link Alert}'s.
     *
     * @return list of updated alerts.
     */
    public Map<Route, List<Alert>> getUpdatedAlerts() {
        return mUpdatedAlerts;
    }

    /**
     * Get all alerts, which should be considered stale)
     * @return list of stale alerts.
     */
    public Map<Route, List<Alert>> getStaleAlerts() {
        return mStaleAlerts;
    }

    /**
     * Get the agencyId for these alert modifications.
     *
     * @return Agency identifier.
     */
    public String getAgencyId() {
        return mAgencyId;
    }
}
