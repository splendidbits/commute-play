package models.alerts;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "agencies")
public class Agency extends Model {
    public static Finder<Integer, Agency> find = new Model.Finder<>(Integer.class, Agency.class);

    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Id
    public Integer id;

    @OneToMany(mappedBy = "agency")
    public List<Route> routes;

    public String agencyName;
}
