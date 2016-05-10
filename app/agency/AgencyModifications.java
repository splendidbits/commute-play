package agency;

import models.alerts.Alert;
import models.alerts.Route;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of new, uddated, or removed alerts for an
 * {@link models.alerts.Agency}
 */
public class AgencyModifications {
    private int mAgencyId;
    private List<Route> mUpdatedAlertRoutes = new ArrayList<>();
    private List<Route> mStaleAlertRoutes = new ArrayList<>();

    @SuppressWarnings("unused")
    private AgencyModifications() {
    }

    /**
     * Create a alert modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public AgencyModifications(int agencyId) {
        mAgencyId = agencyId;
    }

    /**
     * Check to see if there are modified alerts for an route / agency.
     * These include alerts which are  new, updated, or stale.
     *
     * @return true if the agency has updated alerts within this model.
     */
    public boolean hasModifiedRoutes() {
        return !mUpdatedAlertRoutes.isEmpty() || !mStaleAlertRoutes.isEmpty();
    }

    /**
     * Add a list of route alerts as new, updated or stale. It will not be added if it has already
     * been added as stale.
     *
     * @param routes The routes with alerts to set as updated or stale.
     */
    public void addRoute(@Nonnull List<Route> routes) {
        // Do not add new alert if it exists in the stale collection.
        for (Route route : routes) {
            addRoute(route);
        }
    }

    /**
     * Add a route as stale, new, or updated. It will not be added if it has already
     * been added as stale.
     *
     * @param route The route with alerts to flag as updated or new.
     */
    public void addRoute(@Nonnull Route route) {
        if (route.routeId != null && route.alerts != null) {
            boolean addRouteAsUpdated = false;

            // If all alerts are empty, this is a stale route.
            for (Alert updatedAlert : route.alerts) {
                if (updatedAlert.messageBody != null && !updatedAlert.messageBody.isEmpty()) {
                    addRouteAsUpdated = true;
                    break;
                }
            }

            // Check that the route doesn't already exist when adding to updated or stale collections.
            if (addRouteAsUpdated && !mUpdatedAlertRoutes.contains(route)) {
                mUpdatedAlertRoutes.add(route);

            } else if (!addRouteAsUpdated && !mStaleAlertRoutes.contains(route)) {
                mStaleAlertRoutes.add(route);
            }
        }
    }

    /**
     * Get a list of all the agency routes than contain new or updated alerts.
     *
     * @return list of routes with updated alerts.
     */
    public List<Route> getUpdatedAlertRoutes() {
        return mUpdatedAlertRoutes;
    }

    /**
     * Get a list of all the agency routes than contain stale or removed alerts.
     *
     * @return list of routes with stale alerts.
     */
    public List<Route> getStaleAlertRoutes() {
        return mStaleAlertRoutes;
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
