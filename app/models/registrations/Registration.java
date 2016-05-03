package models.registrations;

import com.avaje.ebean.Model;
import main.Constants;
import models.accounts.Account;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "registrations", schema = "device_information")
public class Registration extends Model {
    public static Finder<String, Registration> find = new Model.Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Registration.class);

    @Id
    @Column(name = "id")
	public String id;

    @Column(name = "registration_token")
    public String registrationToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "account_id",
            table = "api_accounts.accounts",
            referencedColumnName = "id",
            unique = false,
            updatable = false)
    public Account account;

    @Nullable
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL)
    public List<Subscription> subscriptions;

    @Basic
    @Column(name = "time_registered", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeRegistered = Calendar.getInstance();

    @SuppressWarnings("unused")
    public Registration() {
    }

    public Registration(@Nonnull String deviceId, @Nonnull String registrationToken) {
        this.id = deviceId;
        this.registrationToken = registrationToken;
    }

    @PrePersist
    public void initialValues() {
        timeRegistered = Calendar.getInstance();
    }
}
