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

    @Id
    @Column(name = "route_id")
    public String routeId;

    @Column(name = "route_name")
    public String routeName;

    @ManyToOne(fetch = FetchType.EAGER)
    public Agency agency;

    @ManyToMany(mappedBy = "routes", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    public List<Subscription> subscriptions;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Alert> alerts;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route otherRoute = (Route) obj;

            boolean routeDetailsMatch =
                    routeId.equals(otherRoute.routeId) &&
                    routeName.equals(otherRoute.routeName);

            if (routeDetailsMatch) {

                if (alerts == null && otherRoute.alerts == null) {
                    return true;
                }

                if (alerts != null && otherRoute.alerts != null) {
                    // If both alert sets are identical.
                    return alerts.equals(otherRoute.alerts);
                }
            }
            return false;
        }
        return obj.equals(this);
    }

    @Override
    public int compareTo(Route o) {
        return routeId.compareTo(o.routeId);
    }

    public Route() {
    }

    public Route(String routeId) {
        this.routeId = routeId;
    }

    public Route(String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
    }
}
