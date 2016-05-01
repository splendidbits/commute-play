package models.registrations;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Route;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "subscriptions", schema = "device_subscriptions")
public class Subscription extends Model {
    public static Finder<Long, Subscription> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "subscription_id")
    public Long id;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @Column(name = "device_id")
    public Registration registration;

    @ManyToMany(cascade = CascadeType.REFRESH)
    @JoinTable(
            name="subscription_route",
            schema = "device_subscriptions",
            joinColumns=@JoinColumn(
                    name="subscription_id",
                    referencedColumnName="subscription_id",
                    unique = true,
                    nullable = false,
                    insertable = true,
                    updatable = false),

            inverseJoinColumns=@JoinColumn(
                    name="route_id",
                    table = "agency_alerts.routes",
                    referencedColumnName="id",
                    unique = true,
                    nullable = true,
                    insertable = true,
                    updatable = false))
    public List<Route> routes;

    @Basic
    @Column(name = "time_subscribed", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed;

    @Override
    public void insert() {
        timeSubscribed = Calendar.getInstance();
        super.insert();
    }
}
