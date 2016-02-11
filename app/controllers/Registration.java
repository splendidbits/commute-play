package controllers;

import models.Person;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import java.util.List;

import static play.libs.Json.toJson;

public class Registration extends Controller {

    public Result index() {
        return ok(index.render());
    }

}
