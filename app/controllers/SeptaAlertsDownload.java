package controllers;

import agencies.septa.FetchSeptaAlerts;
import play.mvc.Controller;
import play.mvc.Result;

public class SeptaAlertsDownload extends Controller {

    /**
     * Download SEPTA alerts from the json server and send them to the
     * dispatch processes.
     *
     * Download current alerts.
     * 1: Download agency alerts.
     * 2: Bundle into standard format.
     *
     * Send to GCM processor
     *
     * 2.5: Go through each Route > Alert bundle and find any differences
     * 3: Collect the new alerts
     * 4: Persist new data
     * 5: Get list of subscriptions for route
     * 6: send data in batches of 1000 to google.
     *
     * @return Result.
     */
    public Result downloadAlerts() {
        FetchSeptaAlerts.process();
        return ok();
    }
}
