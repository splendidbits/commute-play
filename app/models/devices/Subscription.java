package models.devices;

import com.avaje.ebean.Model;
import models.alerts.Route;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name = "subscriptions", schema = "device_information")
public class Subscription extends Model {
    public static Finder<Long, Subscription> find = new Model.Finder<>(Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "id")
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "device_id",
            table = "device_information.devices",
            referencedColumnName = "id",
            nullable = false,
            unique = false,
            updatable = false)
    public Device device;

    @ManyToOne(fetch = FetchType.EAGER)
    @Column(name = "route_id")
    @JoinColumn(
            name = "route_id",
            table = "agency_alerts.routes",
            referencedColumnName = "id",
            nullable = true,
            unique = false,
            updatable = false)
    public Route route;

    @Basic
    @Column(name = "time_subscribed", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date timeSubscribed;

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
