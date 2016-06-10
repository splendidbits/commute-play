package controllers;

import play.mvc.Controller;
import play.mvc.Result;

@SuppressWarnings("unused")
public class Application extends Controller {

    public Result index() {
        return ok();
    }

    public Result robots() {
        return ok("User-agent: *\nDisallow: /");
    }
}
