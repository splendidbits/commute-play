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
    public String id;

    @Column(name = "route_name")
    public String routeName;

    @ManyToOne(fetch = FetchType.EAGER)
    public Agency agency;

    @ManyToMany(mappedBy = "routes", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    public List<Subscription> subscriptions;

    @OneToMany(mappedBy = "route", cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    public List<Alert> alerts;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route otherRoute = (Route) obj;

            boolean routeDetailsMatch = id.equals(otherRoute.id) &&
                    routeName.equals(otherRoute.routeName);

            if (routeDetailsMatch) {

                // Both sets of route alerts are null.
                if (alerts == null && otherRoute.alerts == null) {
                    return true;
                }

                // Both sets of route alerts are non-null.
                if (alerts != null && otherRoute.alerts != null) {
                    for (Alert alert : alerts) {
                        if (!otherRoute.alerts.contains(alert)) {
                            return false;
                        }
                    }
                    return true;
                }
            }

            return false;
        }
        return obj.equals(this);
    }

    @Override
    public int compareTo(Route o) {
        return routeName.compareTo(o.routeName);
    }

    public Route() {
    }

    public Route(String routeId) {
        this.id = routeId;
    }

    public Route(String routeId, String routeName) {
        this.id = routeId;
        this.routeName = routeName;
    }
}
