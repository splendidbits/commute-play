package models.devices;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import io.ebean.Finder;
import io.ebean.Model;
import models.alerts.Route;

@Entity
@Table(name = "subscriptions", schema = "device_information")
public class Subscription extends Model {
    public static Finder<Long, Subscription> find = new Finder<>(Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "id")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "device_id",
            table = "device_information.devices",
            referencedColumnName = "id",
            nullable = false,
            unique = false,
            updatable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "route_id")
    @JoinColumn(
            name = "route_id",
            table = "agency_alerts.routes",
            referencedColumnName = "id",
            updatable = false)
    private Route route;

    @Basic
    @Column(name = "time_subscribed", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeSubscribed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Date getTimeSubscribed() {
        return timeSubscribed;
    }

    public void setTimeSubscribed(Date timeSubscribed) {
        this.timeSubscribed = timeSubscribed;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += device != null
                ? device.hashCode()
                : hashCode;

        hashCode += route != null
                ? route.hashCode()
                : hashCode;

        hashCode += timeSubscribed != null
                ? timeSubscribed.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @PrePersist
    public void prePersist(){
        timeSubscribed = new Date();
    }
}
