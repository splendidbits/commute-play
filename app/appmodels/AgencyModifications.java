package appmodels;

import models.alerts.Route;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
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
     * Add a list of route alerts as new or updated. It will not be added if it has already
     * been added as stale.
     *
     * @param routes The routes with alerts to set as updated or new.
     */
    public void addUpdatedRouteAlert(@Nonnull List<Route> routes) {
        // Do not add new alert if it exists in the stale collection.
        for (Route route : routes) {
            addUpdatedRouteAlert(route);
        }
    }

    /**
     * Add a route as new or updated. It will not be added if it has already
     * been added as stale.
     *
     * @param route The route with alerts to flag as updated or new.
     */
    public void addUpdatedRouteAlert(@Nonnull Route route) {
        if (route.routeId != null) {

            // Remove matching stale alert if alert with the route Id exists.
            Iterator<Route> staleRouteIterator = mStaleAlertRoutes.iterator();
            while (staleRouteIterator.hasNext()) {

                Route staleRoute = staleRouteIterator.next();
                if (staleRoute.routeId.equals(route.routeId)) {
                    staleRouteIterator.remove();
                }
            }

            mUpdatedAlertRoutes.add(route);
        }
    }

    /**
     * Add a list of route alerts as stale. It will not be added if it has already
     * been added as new or updated.
     *
     * @param routes The routes with alerts to flag as stale or missing.
     */
    public void addStaleRouteAlert(@Nonnull List<Route> routes) {
        // Do not add stale alert if it exists in the updated collection.
        for (Route staleRoute : routes) {
            addStaleRouteAlert(staleRoute);
        }
    }

    /**
     * Add a route alerts as stale. It will not be added if it has already
     * been added as new or updated.
     *
     * @param route The route with alerts to flag as stale or missing.
     */
    public void addStaleRouteAlert(@Nonnull Route route) {
        if (route.routeId != null) {

            // Do not add stale alert if it exists in new alerts list.
            for (Route updatedRoute : mUpdatedAlertRoutes) {
                if (updatedRoute.routeId.equals(route.routeId)){
                    return;
                }
            }

            mStaleAlertRoutes.add(route);
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
