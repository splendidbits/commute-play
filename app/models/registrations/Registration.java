package models.registrations;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "registrations", schema = "public")
public class Registration extends Model {
    public static Finder<String, Registration> find = new Model.Finder<>("route_alerts", String.class, Registration.class);

    @Id
    @Column(name = "id")
	public String deviceId;

    @Column(name = "registration_id")
    public String registrationId;

    @Basic
    @Column(name="time_registered")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeRegistered;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "registration")
    public List<Subscription> subscriptions;
}
