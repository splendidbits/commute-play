package models.registrations;

import com.avaje.ebean.Model;
import models.alerts.Alert;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "subscriptions", schema = "public")
public class Subscription extends Model {
    public static Finder<Integer, Subscription> find = new Model.Finder<>("route_alerts", Integer.class, Subscription.class);

    @Id
    @SequenceGenerator(name="subscriptions_id_seq_gen", sequenceName="public.subscriptions_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.subscriptions_id_seq_gen")
    @Column(name = "id")
    public Integer subscriptionId;

    @ManyToOne
    @Column(name = "device_id")
    public Registration registration;

    @ManyToMany(mappedBy = "subscriptions", fetch=FetchType.LAZY)
    public List<Alert> alerts;

    @Basic
    @Column(name="time_subscribed")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed;
}
