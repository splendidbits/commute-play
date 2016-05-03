package models.registrations;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Route;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "registration_id",
            table = "device_information.registrations",
            referencedColumnName = "id",
            unique = false,
            updatable = false)
    public Registration registration;

    @Nullable
    @ManyToMany(fetch = FetchType.EAGER)
    public List<Route> routes;

    @Basic
    @Column(name = "time_subscribed", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed = Calendar.getInstance();

    @SuppressWarnings("unused")
    public Subscription() {
    }
}
