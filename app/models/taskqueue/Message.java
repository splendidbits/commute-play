package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.EnumValue;
import enums.PlatformType;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "messages", schema = "task_queue")
public class Message extends Model {
    public static Finder<Long, Message> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Message.class);

    public enum Priority {
        @EnumValue("low")
        PRIORITY_LOW,

        @EnumValue("normal")
        PRIORITY_NORMAL,

        @EnumValue("high")
        PRIORITY_HIGH,
    }

    @Id
    @Column(name = "message_id")
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    public Task task;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "message", fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "message", fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Basic
    @Column(name = "sent_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    public Priority priority;

    @Column(name = "time_to_live")
    public int ttl;

    @Column(name = "restricted_package_name")
    public String restrictedPackageName;

    @Column(name = "delay_while_idle")
    public boolean shouldDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Column(name = "auth_token")
    public String authToken;

    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    public PlatformType platformType;

    @Column(name = "endpoint_url")
    public String endpointUrl;

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

    @PrePersist
    public void initialValues() {
        sentTime = Calendar.getInstance();
    }

    public Message() {
        priority = Priority.PRIORITY_NORMAL;
        ttl = 86400;
    }
}
