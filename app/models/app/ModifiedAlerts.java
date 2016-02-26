package models.app;

import models.alerts.Alert;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for added / removed alerts.
 */
public class ModifiedAlerts {
    public int mAgencyId;
    public List<Alert> mAddedRouteAlerts = new ArrayList<>();
    public List<Alert> mRemovedRouteAlerts = new ArrayList<>();

    public ModifiedAlerts(int agencyId) {
        mAgencyId = agencyId;
    }

    public boolean hasModifiedAlerts() {
        return !mAddedRouteAlerts.isEmpty() || !mRemovedRouteAlerts.isEmpty();
    }
}
