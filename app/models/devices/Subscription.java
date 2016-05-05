package models.devices;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Route;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "subscriptions", schema = "device_information")
public class Subscription extends Model {
    public static Finder<Long, Subscription> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "id")
    public Long id;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(
            name = "device_id",
            table = "device_information.devices",
            referencedColumnName = "id",
            nullable = false,
            unique = false,
            updatable = true)
    public Device device;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @Column(name = "route_id")
    @JoinColumn(
            name = "route_id",
            table = "agency_updates.routes",
            referencedColumnName = "id",
            nullable = true,
            unique = false,
            updatable = true)
    public Route route;

    @Basic
    @Column(name = "time_subscribed", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed = Calendar.getInstance();

    @SuppressWarnings("unused")
    public Subscription() {
    }
}