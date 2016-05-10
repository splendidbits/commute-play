package models.devices;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import main.Constants;
import models.accounts.Account;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "devices", schema = "device_information")
public class Device extends Model {
    public static Finder<Long, Device> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Device.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "device_id_seq_gen", sequenceName = "device_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_id_seq_gen")
    public Long id;

    @Column(name = "device_id", columnDefinition = "TEXT")
	public String deviceId;

    @Column(name = "token", columnDefinition = "TEXT")
    public String token;

    @Column(name = "app_key")
    public String appKey;

    @Column(name = "user_key")
    public String userKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "account_id",
            table = "api_accounts.accounts",
            referencedColumnName = "id",
            nullable = true,
            unique = false,
            updatable = true)
    public Account account;

    @JsonIgnore
    @Nullable
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Subscription> subscriptions;

    @Basic
    @Column(name = "time_registered", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeRegistered = Calendar.getInstance();

    @SuppressWarnings("unused")
    public Device() {
    }

    public Device(@Nonnull String deviceId, @Nonnull String token) {
        this.deviceId = deviceId;
        this.token = token;
    }

    @PrePersist
    public void initialValues() {
        timeRegistered = Calendar.getInstance();
    }
}
