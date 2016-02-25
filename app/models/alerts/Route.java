package models.alerts;

import com.avaje.ebean.Model;
import main.Constants;
import models.registrations.Subscription;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes", schema = "agency_alerts")
public class Route extends Model {
    public static Finder<String, Route> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Route.class);

    @Id
    @Column(name = "id")
    public String routeId;

    @Column(name = "route_name")
    public String routeName;

    @ManyToOne(fetch=FetchType.LAZY)
    public Agency agency;

    @ManyToMany(mappedBy="routes")
    public List<Subscription> subscriptions;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "route")
    public List<Alert> alerts;
}
