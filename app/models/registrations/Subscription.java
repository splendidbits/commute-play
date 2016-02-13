package models.registrations;

import com.avaje.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "subscriptions")
public class Subscription extends Model {
    public static Finder<String, Subscription> find = new Model.Finder<>(String.class, Subscription.class);

    @Id
    public String devuId;

    @ManyToOne
    public Registration registration;

    public String timeSubscribed;
}
