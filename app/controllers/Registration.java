package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

/**
 * The public API for registering devices with the commute server.
 */
public class Registration extends Controller {

    public Result index() {
        // Grab the header that the client has sent.
        String userAgent = request().getHeader("User-Agent");

        String registrationId = request().getQueryString("devregid");
        String deviceId = request().getQueryString("devuuid");



        return ok(index.render());
    }

}
