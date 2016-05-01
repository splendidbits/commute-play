package appmodels;

import models.alerts.Alert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for added / removed alerts.
 */
public class ModifiedAlerts {
    private int mAgencyId;
    private List<Alert> mUpdatedRouteAlerts = new ArrayList<>();
    private List<Alert> mStaleRouteAlerts = new ArrayList<>();

    private ModifiedAlerts() {
    }

    public ModifiedAlerts(int agencyId) {
        mAgencyId = agencyId;
    }

    public boolean hasModifiedAlerts() {
        return !(mUpdatedRouteAlerts.isEmpty() && !mStaleRouteAlerts.isEmpty());
    }

    public void addUpdatedRouteAlert(@Nonnull Alert updatedAlert) {
        mUpdatedRouteAlerts.add(updatedAlert);
    }

    public void addStaleRouteAlert(@Nonnull Alert staleAlert) {
        mStaleRouteAlerts.add(staleAlert);
    }

    public List<Alert> getUpdatedAlerts() {
        return mUpdatedRouteAlerts;
    }

    public List<Alert> getStaleRouteAlerts() {
        return mStaleRouteAlerts;
    }

    public int getAgencyId() {
        return mAgencyId;
    }
}
