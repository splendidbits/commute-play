package models;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import models.alerts.Alert;
import models.alerts.Route;

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
        for (Map.Entry<Route, List<Alert>> entry : mUpdatedAlerts.entrySet()) {
            if (!CollectionUtils.isEmpty(entry.getValue())) {
                return true;
            }
        }

        for (Map.Entry<Route, List<Alert>> entry : mStaleAlerts.entrySet()) {
            if (!CollectionUtils.isEmpty(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new, or updated alert.
     *
     * @param alert The alert to flag as updated or new.
     */
    public void addUpdatedAlert(@Nonnull Route route, @Nonnull Alert alert) {
        Route routeKey = null;
        for (Map.Entry<Route, List<Alert>> entry : mUpdatedAlerts.entrySet()) {
            if (entry.getKey().getRouteId().equals(route.getRouteId())) {
                routeKey = entry.getKey();
            }
        }

        if (routeKey != null) {
            List<Alert> alerts = new ArrayList<>(getUpdatedAlerts(routeKey.getRouteId()));
            alerts.add(alert);
            mUpdatedAlerts.put(routeKey, alerts);
        } else {
            mUpdatedAlerts.put(route, Collections.singletonList(alert));
        }
    }

    /**
     * Add an alert that is stale (used to exist but no longer does.
     *
     * @param alert The alert which is deemed stale.
     */
    public void addStaleAlert(@Nonnull Route route, @Nonnull Alert alert) {

        Route routeKey = null;
        for (Map.Entry<Route, List<Alert>> entry : mStaleAlerts.entrySet()) {
            if (entry.getKey().getRouteId().equals(route.getRouteId())) {
                routeKey = entry.getKey();
            }
        }

        if (routeKey != null) {
            List<Alert> alerts = new ArrayList<>(getStaleAlerts(routeKey.getRouteId()));
            alerts.add(alert);
            mStaleAlerts.put(routeKey, alerts);
        } else {
            mStaleAlerts.put(route, Collections.singletonList(alert));
        }
    }

    public Set<Route> getUpdatedAlertRoutes() {
        return mUpdatedAlerts.keySet();
    }

    public Set<Route> getStaleAlertRoutes() {
        return mStaleAlerts.keySet();
    }

    /**
     * Get a list of all new or updated Agency {@link Alert}'s.
     *
     * @return list of updated alerts.
     */
    public List<Alert> getUpdatedAlerts(String routeId) {
        if (routeId != null) {
            for (Map.Entry<Route, List<Alert>> entry : mUpdatedAlerts.entrySet()) {
                if (entry.getKey().getRouteId().equals(routeId)) {
                    return entry.getValue();
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Get all alerts, which should be considered stale)
     *
     * @return list of stale alerts.
     */
    public List<Alert> getStaleAlerts(String routeId) {
        if (routeId != null) {
            for (Map.Entry<Route, List<Alert>> entry : mStaleAlerts.entrySet()) {
                if (entry.getKey().getRouteId().equals(routeId)) {
                    return entry.getValue();
                }
            }
        }
        return new ArrayList<>();
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
