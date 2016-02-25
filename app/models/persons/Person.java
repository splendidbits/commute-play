package models.persons;

import com.avaje.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "persons", schema = "public")
public class Person extends Model{
    public static Finder<Integer, Person> find = new Model.Finder<>(Integer.class, Person.class);

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Integer id;

    public String name;

}
