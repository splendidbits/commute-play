package services;

import models.alerts.Agency;
import play.db.ebean.Transactional;

public class AlertsDatabaseService {

    /**
     * Save a bundle of agency route alerts to the datastore, clearing out the previous set.
     * @param agencyRouteAlerts list of route alerts.
     * @return boolean for success.
     */
    @Transactional
    public boolean saveRouteAlerts(Agency agencyRouteAlerts) {
        if (agencyRouteAlerts != null) {
            agencyRouteAlerts.find.deleteById(agencyRouteAlerts.id);
            agencyRouteAlerts.save();

//            JPA.em().createQuery("delete from agency where 'agencyId' = " + agencyRouteAlerts.agencyId);
//            JPA.em().persist(agencyRouteAlerts);
        }
        return false;
    }

}
