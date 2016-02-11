package models;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes_table")
public class Route extends Model {
    public static Finder<String, Route> find = new Model.Finder<>(String.class, Route.class);

    @Id
	public String routeId;

    public String routeName;

    @OneToMany(mappedBy = "route")
    public List<Alert> routeAlerts;

    @ManyToOne
    public Agency agency;
}
