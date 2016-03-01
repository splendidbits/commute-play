package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import models.registrations.Subscription;

import javax.persistence.*;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "routes", schema = "agency_alerts")
public class Route extends Model implements Comparable<Route> {
    public static Finder<String, Route> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Route.class);

    public Route() {
    }

    public Route(String routeId) {
        this.routeId = routeId;
    }

    public Route(String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
    }

    @Id
    @Column(name = "route_id")
    public String routeId;

    @Column(name = "route_name")
    public String routeName;

    @ManyToOne(fetch = FetchType.LAZY)
    public Agency agency;

    @ManyToMany(mappedBy = "routes", cascade = CascadeType.REFRESH)
    public List<Subscription> subscriptions;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "route")
    public List<Alert> alerts;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route otherRoute = (Route) obj;

            boolean routeDetailsMatch = routeId == otherRoute.routeId && routeName == otherRoute.routeName;
            boolean alertsMatch = false;

            if (alerts != null) {
                alertsMatch = alerts.equals(otherRoute.alerts);
            }

            if (otherRoute.alerts != null) {
                alertsMatch = otherRoute.alerts.equals(alerts);
            }

            return routeDetailsMatch && alertsMatch;
        }
        return obj.equals(this);
    }

    @Override
    public int compareTo(Route o) {
        return routeId.compareTo(o.routeId);
    }
}
