package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import enums.TransitType;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.PrivateOwned;
import models.devices.Subscription;

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
    private List<Alert> alerts;

    @ManyToOne
    private Agency agency;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "transit_type")
    @Enumerated(EnumType.STRING)
    private TransitType transitType;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_sticky")
    private Boolean isSticky = false;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    private String externalUri;

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

            boolean sameTransitType = CompareUtils.isEquals(transitType, other.transitType);

            boolean sameDefaults = CompareUtils.isEquals(isDefault, other.isDefault);

            boolean sameUri = CompareUtils.isEquals(externalUri, other.externalUri);

            boolean sameAlerts = CompareUtils.isEquals(alerts, other.alerts);

            return sameRouteId && sameRouteName && sameTransitType && sameDefaults && sameUri && sameAlerts;
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
