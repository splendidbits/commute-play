package models.alerts;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes")
public class Route extends Model {
    public static Finder<String, Route> find = new Model.Finder<>("route_alerts", String.class, Route.class);

    @Id
	public String routeId;

    public String routeName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "route")
    public List<Alert> routeAlerts;

    @ManyToOne
    public Agency agency;
}
