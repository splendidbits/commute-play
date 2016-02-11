package controllers;

import main.LogConfig;
import models.Person;
import play.data.Form;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import services.Logger;
import views.html.index;

import java.util.List;

import static play.libs.Json.toJson;

public class Application extends Controller {

    public Result index() {
        return ok(index.render());
    }

    @Transactional
    public Result addPerson() {
        Person person = Form.form(Person.class).bindFromRequest().get();
        person.save();
        return redirect(routes.Application.index());
    }

    @Transactional
    public Result getPersons() {
        Logger logger = Logger.getInstance(new LogConfig());
        List<Person> persons = Person.find.all();

        for (Person person : persons) {
            logger.d(person.name);
        }
        return ok(toJson(persons));
    }
}
