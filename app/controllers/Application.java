package controllers;

import services.splendidlog.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

@SuppressWarnings("unused")
public class Application extends Controller {

    private static final String TAG = Application.class.getSimpleName();

    public Result index() {
        return ok();
    }

    public Result robots() {
        return ok("User-agent: *\nDisallow: /");
    }
}
