package controllers;

import main.Log;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class Application extends Controller {

    private static final String TAG = Application.class.getSimpleName();

    @Inject
    private Log mLog;

    public Result index() {
        return ok();
    }
}
