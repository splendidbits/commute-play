package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Route;
import models.registrations.Subscription;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "account", schema = "service_accounts")
public class Account extends Model {
    public static Finder<Integer, Account> find = new Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Account.class);

    public Account() {
    }

    @Id
    @SequenceGenerator(name = "account_id_seq_gen", sequenceName = "account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_id_seq_gen")
    @Column(name = "account_id")
    public Integer accountId;

    @OneToMany(cascade = CascadeType.REFRESH, mappedBy = "account", fetch = FetchType.LAZY)
    public List<Message> messages;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "account", fetch = FetchType.LAZY)
    public List<PlatformAccount> platformAccounts;

    @Column(name = "organisation_name")
    public String orgName;

    @Column(name = "account_email")
    public String email;

    @Column(name = "password_hash")
    public String passwordHash;

    @Column(name = "api_key")
    public String apiKey;

    @Column(name = "estimated_limit_pm")
    public Long estSendLimit;

    @Column(name = "message_limit_pm")
    public Long monthlySendLimit;

    @Column(name = "active", columnDefinition = "boolean default false")
    public boolean active;

    @Basic
    @Column(name = "time_enrolled")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeEnrolled = Calendar.getInstance();
}