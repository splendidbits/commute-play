package controllers;

import main.Log;
import models.persons.Person;
import play.data.Form;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import java.util.List;

import static play.libs.Json.toJson;

public class Application extends Controller {

    public Result index() {
        return ok(index.render());
    }

    @Transactional
    public Result addPerson() {
        Form<Person> persons = Form.form(Person.class).bindFromRequest();
        Person person = persons.get();
        Log.getInstance().d("Saving name: " + person.name);
        person.save();
        return redirect(routes.Application.index());
    }

    @Transactional
    public Result getPersons() {
        Log log = Log.getInstance();
        List<Person> persons = Person.find.all();

        for (Person person : persons) {
            log.d("Found name: " + person.name);
        }
        return ok(toJson(persons));
    }
}
