package models.app;

import models.alerts.Alert;
import models.alerts.Route;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for added / removed alerts.
 */
public class ModifiedAlerts {
    private int mAgencyId;
    private Map<Route, List<Alert>> mUpdatedRouteAlerts = new HashMap<>();
    private Map<Route, List<Alert>> mStaleRouteAlerts = new HashMap<>();

    private ModifiedAlerts() {
    }

    public ModifiedAlerts(int agencyId) {
        mAgencyId = agencyId;
    }

    public boolean hasModifiedAlerts() {
        return !mUpdatedRouteAlerts.isEmpty() || !mStaleRouteAlerts.isEmpty();
    }

    public void addUpdatedRouteAlert(@Nonnull Alert updatedAlert, @Nonnull Route route) {
        List<Alert> alerts = mStaleRouteAlerts.containsKey(route)
                ? mStaleRouteAlerts.get(route)
                : new ArrayList<>();

        alerts.add(updatedAlert);
        mUpdatedRouteAlerts.put(route, alerts);
    }

    public void addStaleRouteAlert(@Nonnull Alert staleAlert, @Nonnull Route route) {
        List<Alert> alerts = mStaleRouteAlerts.containsKey(route)
                ? mStaleRouteAlerts.get(route)
                : new ArrayList<>();

        alerts.add(staleAlert);
        mStaleRouteAlerts.put(route, alerts);
    }

    public Map<Route, List<Alert>> getUpdatedAlerts() {
        return mUpdatedRouteAlerts;
    }

    public List<Alert> getStaleRouteAlerts(@Nonnull Route route) {
        return mStaleRouteAlerts.get(route);
    }
}
