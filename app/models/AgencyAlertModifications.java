package models;

import models.alerts.Route;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of new, uddated, or removed alerts for an {@link models.alerts.Agency}
 */
public class AgencyAlertModifications {
    private int mAgencyId;
    private List<Route> mUpdatedRoutes = new ArrayList<>();
    private List<Route> mStaleRoutes = new ArrayList<>();

    /**
     * Create a modifications model for a specific agency.
     *
     * @param agencyId Identifier of agency for updated or stale alerts.
     */
    public AgencyAlertModifications(int agencyId) {
        mAgencyId = agencyId;
    }

    /**
     * Check to see if there are modified routes (either new / stale routes / alerts).
     * These include alerts which are  new, updated, or stale.
     *
     * @return true if the agency has updated routes (either stale or new).
     */
    public boolean hasUpdatedRoutes() {
        return !mUpdatedRoutes.isEmpty() || !mStaleRoutes.isEmpty();
    }

    /**
     * Add a new, or updated alert.
     *
     * @param route The route to flag as updated or new.
     */
    public void addUpdatedRoute(@Nonnull Route route) {
        if (route != null && route.routeId != null && !route.routeId.isEmpty()) {
            mUpdatedRoutes.add(route);
        }
    }

    /**
     * Add a route that is stale
     *
     * @param route The route which is deemed stale.
     */
    public void addStaleRoute(@Nonnull Route route) {
        if (route != null && route.routeId != null && !route.routeId.isEmpty()) {
            mStaleRoutes.add(route);
        }
    }

    /**
     * Get a list of all new or updated Agency {@link Route}'s.
     *
     * @return list of updated routes.
     */
    public List<Route> getUpdatedRoutes() {
        return mUpdatedRoutes;
    }

    /**
     * Get all routes, with alerts, which should be considered stale)
     * @return list of stale routes.
     */
    public List<Route> getStaleRoutes() {
        return mStaleRoutes;
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
