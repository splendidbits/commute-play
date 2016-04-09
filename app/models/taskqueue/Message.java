package models.taskqueue;

import com.avaje.ebean.Model;
import dispatcher.interfaces.PlatformMessage;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "messages", schema = "task_queue")
public class Message extends Model implements PlatformMessage {
    public static Finder<Integer, Message> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Message.class);

    @Id
    @Column(name = "message_id")
    //@SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Integer messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    public Task task;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "message", fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "message", fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "sent_time")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime;

    @Column(name = "priority")
    public String priority;

    @Column(name = "time_to_live")
    public int ttl;

    @Column(name = "restricted_package_name")
    public String restrictedPackageName;

    @Column(name = "delay_while_idle")
    public boolean isDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Column(name = "auth_token")
    public String authToken;

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

    @Transient
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.PLATFORM_TYPE_GCM;
    }

    @PrePersist
    public void initialValues() {
        sentTime = Calendar.getInstance();
    }

    public Message() {
        priority = "normal";
        ttl = 86400;
    }
}
