package models.registrations;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "subscriptions", schema = "public")
public class Subscription extends Model {
    public static Finder<Integer, Subscription> find = new Model.Finder<>("client_registrations", Integer.class, Subscription.class);

    @Id
    @SequenceGenerator(name="subscriptions_id_seq_gen", sequenceName="public.subscriptions_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.subscriptions_id_seq_gen")
    @Column(name = "id")
    public Integer subscriptionId;

    @ManyToOne
    @Column(name = "device_id")
    public Registration registration;

    @Basic
    @Column(name="time_subscribed")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeSubscribed;
}
