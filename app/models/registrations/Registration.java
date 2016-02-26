package models.registrations;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "registrations", schema = "device_subscriptions")
public class Registration extends Model {
    public static Finder<String, Registration> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Registration.class);

    public Registration() {
    }

    public Registration(String deviceId, String registrationId) {
        this.deviceId = deviceId;
        this.registrationId = registrationId;
    }

    @Id
    @Column(name = "device_id")
	public String deviceId;

    @Column(name = "registration_id")
    public String registrationId;

    @Basic
    @Column(name="time_registered")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeRegistered = Calendar.getInstance();

    @OneToOne(mappedBy = "registration")
    public Subscription subscription;
}
