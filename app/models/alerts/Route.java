package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import enums.RouteFlag;
import enums.TransitType;
import main.Constants;
import models.registrations.Subscription;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "routes", schema = "agency_alerts")
public class Route extends Model implements Comparable<Route> {
    public static Finder<String, Route> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Route.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "route_id_seq_gen", sequenceName = "route_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "route_id_seq_gen")
    public String id;

    @Column(name = "route_id")
    public String routeId;

    @Column(name = "route_name")
    public String routeName;

    @Column(name = "route_flag")
    @Enumerated(EnumType.STRING)
    public RouteFlag routeFlag;

    @Column(name = "transit_type")
    @Enumerated(EnumType.STRING)
    public TransitType transitType;

    @Column(name = "is_default")
    public Boolean isDefault = false;

    @Column(name = "is_sticky")
    public Boolean isSticky = false;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    public String externalUri;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn (name = "agency_id", referencedColumnName = "id")
    public Agency agency;

    @ManyToMany(mappedBy = "routes", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    public List<Subscription> subscriptions;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Alert> alerts = new ArrayList<>();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route otherRoute = (Route) obj;

            boolean sameRouteId = routeId == null && otherRoute.routeId == null ||
                    (routeId != null && routeId.equals(otherRoute.routeId));

            // There is the same basic routeId
            if (sameRouteId) {

                if (alerts == null && otherRoute.alerts == null) {
                    return true;
                }

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
        this.routeId = routeId;
    }

    public Route(String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
    }
}
