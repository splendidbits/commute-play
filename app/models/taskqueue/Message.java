package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.avaje.ebean.annotation.EnumValue;
import main.Constants;
import models.accounts.PlatformAccount;
import interfaces.PlatformMessage;
import models.alerts.Alert;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.*;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "messages", schema = "task_queue")
public class Message extends Model implements PlatformMessage {
    public static Finder<Integer, Message> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Message.class);

    public Message() {
        priority = "normal";
        ttl = 86400;
    }

    @Id
    @Column(name = "message_id")
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Integer messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="task_id")
    public Task task;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "message", fetch = FetchType.LAZY)
    public List<Recipient> recipients;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "message", fetch = FetchType.LAZY)
    public List<PayloadElement> payloadData;

    @Transient
    public PlatformAccount platformAccount;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Column(name = "restricted_package_name")
    public String restrictedPackageName;

    @Column(name = "delay_while_idle")
    public boolean isDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Column(name = "priority")
    public String priority;

    @Column(name = "time_to_live")
    public int ttl;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "sent_time")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime;

    @Transient
    public void addData(String key, String value){
        if (payloadData == null) {
            payloadData = new ArrayList<>();
        }
        PayloadElement payloadElement = new PayloadElement(key, value);
        payloadData.add(payloadElement);
    }

    @Transient
    public void addRegistrationToken(@Nonnull String token){
        Recipient recipient = new Recipient();
        recipient.token = token;
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        recipients.add(recipient);
    }

    @Transient
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.PLATFORM_TYPE_GCM;
    }

    @PrePersist
    public void initialValues() {
        sentTime = Calendar.getInstance();
    }
}
