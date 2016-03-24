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

    private static final String TAG = Application.class.getSimpleName();

    public Result index() {
        return ok();
    }

    public Result personsIndex() {
        return ok(index.render());
    }

    @Transactional
    public Result addPerson() {
        Form<Person> persons = Form.form(Person.class).bindFromRequest();
        Person person = persons.get();
        Log.d(TAG, "Saving name: " + person.name);
        Person.db("persons").save(person);
        return redirect(routes.Application.personsIndex());
    }

    @Transactional
    public Result getPersons() {
        List<Person> persons = Person.db("persons").find(Person.class).findList();
        return ok(toJson(persons));
    }
}
