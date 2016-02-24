package models.alerts;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes", schema = "public")
public class Route extends Model {
    public static Finder<String, Route> find = new Model.Finder<>("route_alerts", String.class, Route.class);

    @Id
    @Column(name = "route_id")
    public String routeId;

    @Column(name = "route_name")
    public String routeName;

    @ManyToOne(fetch=FetchType.LAZY)
    public Agency agency;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "route")
    public List<Alert> alerts;
}
