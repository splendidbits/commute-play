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
    public static Finder<Integer, Subscription> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "subscription_id")
    public Integer subscriptionId;

    @OneToOne
    @Column(name = "registration_id")
    public Registration registration;

    @ManyToMany
    @JoinTable(
            name="subscription_route",
            joinColumns=@JoinColumn(name="subscription_id", referencedColumnName="subscription_id", nullable = true, updatable = true),
            inverseJoinColumns=@JoinColumn(name="route_id", referencedColumnName="route_id", nullable = true, updatable = true))
    public List<Route> routes;

    @Basic
    @Column(name = "time_subscribed")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed;
}
