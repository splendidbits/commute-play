package agency;

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
        return !(mUpdatedAlertRoutes.isEmpty() && mStaleAlertRoutes.isEmpty());
    }

    /**
     * Add a list of routes which contain new or updated alerts.
     *
     * @param routes The routes with alerts to set as updated or new.
     */
    public void addUpdatedRoute(@Nonnull List<Route> routes) {
        for (Route route : routes) {
            addUpdatedRoute(route);
        }
    }

    /**
     * Add a route as new, or updated.
     *
     * @param route The route with alerts to flag as updated or new.
     */
    public void addUpdatedRoute(@Nonnull Route route) {
        if (route.routeId != null && route.alerts != null) {
            mUpdatedAlertRoutes.add(route);
        }
    }

    /**
     * Add a list of routes which contain stale, purged alerts.
     *
     * @param routes The routes with alerts to set as updated or stale.
     */
    public void addStaleRoute(@Nonnull List<Route> routes) {
        for (Route route : routes) {
            addStaleRoute(route);
        }
    }

    /**
     * Add a route which contain stale, purged alerts.
     *
     * @param route The route with alerts to add as stale.
     */
    public void addStaleRoute(@Nonnull Route route) {
        if (route.routeId != null) {
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
