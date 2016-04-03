package models.registrations;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import models.accounts.Account;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "registrations", schema = "device_subscriptions")
public class Registration extends Model {
    public static Finder<String, Registration> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Registration.class);

    @Id
    @Column(name = "device_id")
	public String deviceId;

    @Column(name = "registration_token")
    public String registrationToken;

    @ManyToOne(fetch = FetchType.EAGER)
    public Account account;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.REMOVE)
    public List<Subscription> subscriptions;

    @Basic
    @Column(name = "time_registered")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeRegistered = Calendar.getInstance();

    public Registration() {
        
    }

    public Registration(String deviceId, String registrationToken) {
        this.deviceId = deviceId;
        this.registrationToken = registrationToken;
    }

    @PrePersist
    public void initialValues() {
        timeRegistered = Calendar.getInstance();
    }
}
