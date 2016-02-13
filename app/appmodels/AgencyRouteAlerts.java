package appmodels;

import models.alerts.Agency;
import models.alerts.Route;

import java.util.List;

/**
 * Common model shared between all agencies for
 * setting and getting mRoute alerts
 */
public class AgencyRouteAlerts{
    private Agency mAgency;
    private List<Route> mRoutes;

    public AgencyRouteAlerts(Agency agency, List<Route> routes) {
        mAgency = agency;
        mRoutes = routes;
    }

    public Agency getAgency() {
        return mAgency;
    }

    public List<Route> getRoutes() {
        return mRoutes;
    }
}
