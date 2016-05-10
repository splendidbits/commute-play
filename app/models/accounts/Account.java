package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;
import models.devices.Device;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "accounts", schema = "api_accounts")
public class Account extends Model {
    public static Finder<Long, Account> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Account.class);

    @Id
    @SequenceGenerator(name = "account_id_seq_gen", sequenceName = "account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_id_seq_gen")
    @Column(name = "id")
    public Long id;

    @Nullable
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "account", fetch = FetchType.EAGER)
    public List<PlatformAccount> platformAccounts;

    @Nullable
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "account", fetch = FetchType.LAZY)
    public List<Device> devices;

    @Column(name = "organisation_name")
    public String orgName;

    @Column(name = "account_email")
    public String email;

    @Column(name = "password_hash")
    public String passwordHash;

    @Column(name = "api_key")
    public String apiKey;

    @Column(name = "estimated_limit_day")
    public Long dailyEstLimit;

    @Column(name = "message_limit_day")
    public Long dailySendLimit;

    @Column(name = "active", columnDefinition = "boolean default false")
    public boolean active;

    @Basic
    @Column(name = "time_enrolled", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeEnrolled = Calendar.getInstance();

    @SuppressWarnings("unused")
    public Account() {
    }

    @PrePersist
    public void initialValues() {
        timeEnrolled = Calendar.getInstance();
    }
}
