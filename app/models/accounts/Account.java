package models.accounts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Finder;
import io.ebean.Model;
import models.devices.Device;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "accounts", schema = "api_accounts")
public class Account extends Model {
    public static Finder<Long, Account> find = new Finder<>(Account.class);

    @Id
    @JsonIgnore
    @SequenceGenerator(name = "account_id_seq_gen", sequenceName = "account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_id_seq_gen")
    @Column(name = "id")
    protected Long id;

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
    public Date timeEnrolled;

    @SuppressWarnings("unused")
    public Account() {
    }

    @PrePersist
    public void prePersist() {
        timeEnrolled = new Date();
    }

    @Override
    public int hashCode() {
        if (apiKey != null) {
            return apiKey.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account) {
            Account other = (Account) obj;

            boolean bothApiKeysEmpty = (apiKey == null || other.apiKey.isEmpty()) &&
                    (other.apiKey == null || other.apiKey.isEmpty());

            boolean bothApiKeysMatch = bothApiKeysEmpty || (apiKey != null && apiKey.equals(other.apiKey));

            return bothApiKeysEmpty || bothApiKeysMatch;
        }
        return super.equals(obj);
    }
}
