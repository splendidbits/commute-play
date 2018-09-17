package models.devices;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import io.ebean.Finder;
import io.ebean.Model;
import models.accounts.Account;

@Entity
@Table(name = "devices", schema = "device_information")
public class Device extends Model {
    public static Finder<Long, Device> find = new Finder<>(Device.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "device_id_seq_gen", sequenceName = "device_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_id_seq_gen")
    private Long id;

    @Column(name = "device_id", columnDefinition = "TEXT")
	private String deviceId;

    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "app_key")
    private String appKey;

    @Column(name = "user_key")
    private String userKey;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(
            name = "account_id",
            table = "api_accounts.accounts",
            referencedColumnName = "id")
    private Account account;

    @Nullable
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Subscription> subscriptions;

    @Basic
    @Column(name = "time_registered", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeRegistered;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @Nullable
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(@Nullable List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Date getTimeRegistered() {
        return timeRegistered;
    }

    public void setTimeRegistered(Date timeRegistered) {
        this.timeRegistered = timeRegistered;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Device) {
            Device other = (Device) obj;

            boolean sameDeviceId = deviceId == null && other.deviceId == null ||
                    deviceId != null && deviceId.equals(other.deviceId);

            boolean sameToken = token == null && other.token == null ||
                    token != null && token.equals(other.token);

            boolean sameAppKey = appKey == null && other.appKey == null ||
                    appKey != null && appKey.equals(other.appKey);

            boolean sameUserKey = userKey == null && other.userKey == null ||
                    userKey != null && userKey.equals(other.userKey);

            return sameDeviceId && sameToken && sameAppKey && sameUserKey;
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += deviceId != null
                ? deviceId.hashCode()
                : hashCode;

        hashCode += token != null
                ? token.hashCode()
                : hashCode;

        hashCode += appKey != null
                ? appKey.hashCode()
                : hashCode;

        hashCode += userKey != null
                ? userKey.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @PrePersist
    public void prePersist() {
        timeRegistered = new Date();
    }
}
