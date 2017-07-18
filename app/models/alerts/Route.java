package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.RouteFlag;
import enums.TransitType;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import models.devices.Subscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "routes", schema = "agency_alerts")
public class Route extends Model implements Comparable<Route> {
    public static Finder<String, Route> find = new Finder<>(Route.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "route_id_seq_gen", sequenceName = "route_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "route_id_seq_gen")
    public String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
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

            boolean sameRouteId = CompareUtils.isEquals(routeId, other.routeId);

            boolean sameRouteName = CompareUtils.isEquals(routeName, other.routeName);

            boolean sameRouteFlag = CompareUtils.isEquals(routeFlag, other.routeFlag);

            boolean sameTransitType =CompareUtils.isEquals(transitType, other.transitType);

            boolean sameDefaults = isDefault == other.isDefault;

            boolean sameUri = CompareUtils.isEquals(externalUri, other.externalUri);

            boolean sameAlerts = CompareUtils.isEquals(alerts, other.alerts);

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
        return equals(other) ? -1 : 0;
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
}
