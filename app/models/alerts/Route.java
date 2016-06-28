package models.alerts;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.RouteFlag;
import enums.TransitType;
import models.devices.Subscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes", schema = "agency_alerts")
public class Route extends Model implements Comparable<Route>, Cloneable {
    public static Finder<String, Route> find = new Model.Finder<>(Route.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "route_id_seq_gen", sequenceName = "route_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "route_id_seq_gen")
    public String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agency_id",
            table = "agency_alerts.agencies",
            referencedColumnName = "id")
    public Agency agency;

    @Nullable
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Alert> alerts;

    @JsonIgnore
    @Nullable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "route")
    public List<Subscription> subscriptions;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route other = (Route) obj;

            boolean sameRouteId = routeId == null && other.routeId == null ||
                    routeId != null && routeId.equals(other.routeId);

            boolean sameRouteName = routeName == null && other.routeName == null ||
                    routeName != null && routeName.equals(other.routeName);

            boolean sameRouteFlag = routeFlag == null && other.routeFlag == null ||
                    routeFlag != null && routeFlag.equals(other.routeFlag);

            boolean sameTransitType = transitType == null && other.transitType == null ||
                    transitType != null && transitType.equals(other.transitType);

            boolean sameDefaults = isSticky == other.isSticky && isDefault == other.isDefault;

            boolean sameUri = externalUri == null && other.externalUri == null ||
                    externalUri != null && externalUri.equals(other.externalUri);

            boolean bothAlertsEmpty = alerts == null && other.alerts == null ||
                    alerts == null && other.alerts.isEmpty() ||
                    alerts.isEmpty() && other.alerts == null;

            boolean sameAlerts = bothAlertsEmpty ||
                    alerts != null && alerts != null && other.alerts != null &&
                            (alerts.containsAll(other.alerts) && other.alerts.containsAll(alerts));

            return sameRouteId && sameRouteName && sameRouteFlag && sameTransitType && sameDefaults && sameUri && sameAlerts;
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += routeId != null
                ? routeId.hashCode()
                : hashCode;

        hashCode += routeName != null
                ? routeName.hashCode()
                : hashCode;

        hashCode += routeFlag != null
                ? routeFlag.hashCode()
                : hashCode;

        hashCode += transitType != null
                ? transitType.hashCode()
                : hashCode;

        hashCode += isDefault ? 1 : 0;

        hashCode += externalUri != null
                ? externalUri.hashCode()
                : hashCode;

        hashCode += alerts != null
                ? alerts.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(@NotNull Route other) {
        if (routeId != null && other.routeId != null) {
            return routeId.compareTo(other.routeId);
        }
        return 0;
    }

    @SuppressWarnings("unused")
    protected Route() {
    }

    public Route(@Nonnull String routeId) {
        this.routeId = routeId;
    }

    public Route(@Nonnull String routeId, String routeName) {
        this.routeId = routeId;
        this.routeName = routeName;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        markAsDirty();

        Route route = new Route();
        route.id = id;
        route.routeId = routeId;
        route.routeName = routeName;
        route.transitType = transitType;
        route.isDefault = isDefault;
        route.isSticky = isSticky;
        route.externalUri = externalUri;
        route.agency = agency;
        route.alerts = alerts;
        route.subscriptions = subscriptions;

        return route;
    }
}
