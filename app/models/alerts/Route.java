package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import enums.RouteFlag;
import enums.TransitType;
import main.Constants;
import models.registrations.Subscription;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "routes", schema = "agency_updates")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agency_id",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Agency agency;

    @ManyToMany(mappedBy = "routes", cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @JoinTable(
            name = "route_subscriptions",
            schema = "device_information",
            joinColumns = @JoinColumn(
                    name = "subscription_id",
                    table = "device_information.subscriptions",
                    referencedColumnName = "id",
                    unique = false,
                    nullable = true,
                    insertable = true,
                    updatable = true),

            inverseJoinColumns = @JoinColumn(
                    name = "route_id",
                    table = "agency_updates.routes",
                    referencedColumnName = "id",
                    unique = false,
                    nullable = true,
                    insertable = true,
                    updatable = true))

    public List<Subscription> subscriptions = new ArrayList<>();

    @Nonnull
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Alert> alerts = new ArrayList<>();

    @Override
    public int hashCode() {
        int hashCode = routeId != null
                ? routeId.hashCode()
                : super.hashCode();

        hashCode += routeName != null
                ? routeName.hashCode()
                : hashCode;

        hashCode += routeFlag != null
                ? routeFlag.hashCode()
                : hashCode;

        hashCode += transitType != null
                ? transitType.hashCode()
                : hashCode;

        hashCode += isDefault != null
                ? isDefault.hashCode()
                : hashCode;

        hashCode += isSticky != null
                ? isSticky.hashCode()
                : hashCode;

        hashCode += externalUri != null
                ? externalUri.hashCode()
                : hashCode;

        hashCode += alerts.hashCode();

        return hashCode;
    }

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

            boolean bothAlertsEmpty = alerts.isEmpty() && other.alerts.isEmpty();

            boolean sameAlerts = bothAlertsEmpty || (alerts.containsAll(other.alerts) &&
                    other.alerts.containsAll(alerts));

            return sameRouteId && sameRouteName && sameRouteFlag && sameTransitType &&
                    sameDefaults && sameUri && sameAlerts;
        }
        return obj.equals(this);
    }

    @Override
    public int compareTo(Route o) {
        return routeId.compareTo(o.routeId);
    }

    @SuppressWarnings("unused")
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
