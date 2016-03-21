package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import models.accounts.PlatformAccount;
import interfaces.PlatformMessage;
import models.registrations.Registration;

import javax.persistence.*;
import java.util.*;

import static com.sun.corba.se.spi.activation.IIOP_CLEAR_TEXT.value;

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
    @Column(name = "message_id", updatable = true)
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Integer messageId;

    @ManyToOne(fetch = FetchType.EAGER)
    public Task task;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "message", fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "message", fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @ManyToOne(fetch = FetchType.EAGER)
    public PlatformAccount account;

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

    @Basic
    @Column(name = "message_sent_date")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar messageSent;

    @Transient
    public void addData(String key, String value){
        PayloadElement payloadElement = new PayloadElement(key, value);
        payloadData.add(payloadElement);
    }

    @Transient
    public void addRegistrationId(String registrationId){
        Recipient recipient = new Recipient();
        recipient.recipientId = registrationId;
        if (recipients == null) {
            recipients = new ArrayList<>();
            recipients.add(recipient);
        }
    }

    @Transient
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.PLATFORM_TYPE_GCM;
    }
}
