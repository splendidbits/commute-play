package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.RouteFlag;
import enums.TransitType;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.PrivateOwned;
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
    private String routeId;

    @JsonIgnore
    @OneToMany
    private List<Subscription> subscriptions;

    @PrivateOwned
    @OneToMany(mappedBy = "route", orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
//    @OneToMany(mappedBy = "route", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @JoinColumn(name = "route_id", table = "agency_alerts.alerts", referencedColumnName = "id")
    private List<Alert> alerts;

    @ManyToOne
    private Agency agency;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "route_flag")
    @Enumerated(EnumType.STRING)
    private RouteFlag routeFlag;

    @Column(name = "transit_type")
    @Enumerated(EnumType.STRING)
    private TransitType transitType;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_sticky")
    private Boolean isSticky;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    private String externalUri;

    public Route(@Nonnull String routeId) {
        setRouteId(routeId);
    }

    public Route(@Nonnull String routeId, String routeName) {
        setRouteId(routeId);
        setRouteName(routeName);
    }

    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    @Nullable
    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(@Nullable List<Alert> alerts) {
        this.alerts = alerts;
    }

    @Nullable
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(@Nullable List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public RouteFlag getRouteFlag() {
        return routeFlag;
    }

    public void setRouteFlag(RouteFlag routeFlag) {
        this.routeFlag = routeFlag;
    }

    public TransitType getTransitType() {
        return transitType;
    }

    public void setTransitType(TransitType transitType) {
        this.transitType = transitType;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getSticky() {
        return isSticky;
    }

    public void setSticky(Boolean sticky) {
        isSticky = sticky;
    }

    public String getExternalUri() {
        return externalUri;
    }

    public void setExternalUri(String externalUri) {
        this.externalUri = externalUri;
    }

    @Override
    public int compareTo(@NotNull Route other) {
        return equals(other) ? -1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route) {
            Route other = (Route) obj;

            boolean sameRouteId = CompareUtils.isEquals(routeId, other.routeId);

            boolean sameRouteName = CompareUtils.isEquals(routeName, other.routeName);

            boolean sameRouteFlag = CompareUtils.isEquals(routeFlag, other.routeFlag);

            boolean sameTransitType = CompareUtils.isEquals(transitType, other.transitType);

            boolean sameDefaults = CompareUtils.isEquals(isDefault, other.isDefault);

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
}
