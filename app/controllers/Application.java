package controllers;

import services.splendidlog.Log;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

@SuppressWarnings("unused")
public class Application extends Controller {

    private static final String TAG = Application.class.getSimpleName();

    @Inject
    private Log mLog;

    public Result index() {
        return ok();
    }

    public Result robots() {
        return ok("User-agent: *\nDisallow: /");
    }
}
