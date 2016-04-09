package models.registrations;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import models.alerts.Route;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "subscriptions", schema = "device_subscriptions")
public class Subscription extends Model {
    public static Finder<Integer, Subscription> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Subscription.class);

    @Id
    @SequenceGenerator(name = "subscriptions_id_seq_gen", sequenceName = "subscriptions_id_seq", allocationSize = 1)
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptions_id_seq_gen")
    @Column(name = "subscription_id")
    public Integer subscriptionId;

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
                    nullable = true,
                    insertable = false,
                    updatable = false),

            inverseJoinColumns=@JoinColumn(
                    name="route_id",
                    referencedColumnName="route_id",
                    unique = true,
                    nullable = true,
                    insertable = false,
                    updatable = false))
    public List<Route> routes;

    @Basic
    @Column(name = "time_subscribed")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed;

    @PrePersist
    public void initialValues() {
        timeSubscribed = Calendar.getInstance();
    }
}
