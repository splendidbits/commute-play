package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.enums.MessagePriority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "messages", schema = "push_services")
public class Message extends Model implements Cloneable {
    public static Finder<Long, Message> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Message.class);
    public static final int TTL_SECONDS_DEFAULT = 60 * 60 * 24 * 7;

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "task_id",
            table = "push_services.tasks",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Task task;

    @Nullable
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public Credentials credentials;

    @Nullable
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    public MessagePriority messagePriority = MessagePriority.PRIORITY_NORMAL;

    @Column(name = "ttl_seconds")
    public int ttlSeconds = TTL_SECONDS_DEFAULT;

    @Column(name = "delay_while_idle")
    public boolean shouldDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Column(name = "maximum_retries")
    public int maximumRetries = 10;

    @Basic
    @Column(name = "sent_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime = Calendar.getInstance();

    @Transient
    public void addRecipient(@Nonnull Recipient recipient) {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        recipients.add(recipient);
    }

    @SuppressWarnings("unused")
    public Message() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            Message other = (Message) obj;

            boolean sameCollapseKey = (collapseKey == null && other.collapseKey == null) ||
                    (collapseKey != null && other.collapseKey != null && collapseKey.equals(other.collapseKey));

            boolean sameCredentials = (credentials == null && other.credentials == null) ||
                    (credentials != null && other.credentials != null && credentials.equals(other.credentials));

            boolean samePriority = (messagePriority == null && other.messagePriority == null) ||
                    (messagePriority != null && other.messagePriority != null && messagePriority.equals(other.messagePriority));

            boolean sameTtl = ttlSeconds == other.ttlSeconds;

            boolean sameDelayWhileIdle = shouldDelayWhileIdle == other.shouldDelayWhileIdle;

            boolean sameDryRun = isDryRun == other.isDryRun;

            boolean bothPayloadsEmpty = payloadData == null && other.payloadData == null ||
                    (payloadData != null && payloadData.isEmpty() && other.payloadData != null && other.payloadData.isEmpty());

            boolean samePayloadData = bothPayloadsEmpty || payloadData != null && other.payloadData != null &&
                    (payloadData.containsAll(other.payloadData) && other.payloadData.containsAll(payloadData));

            boolean bothRecipientsEmpty = recipients == null && other.recipients == null ||
                    recipients == null && other.recipients.isEmpty() ||
                    recipients.isEmpty() && other.recipients == null;

            boolean sameRecipients = bothRecipientsEmpty || recipients != other.recipients ||
                    (recipients.containsAll(other.recipients) && other.recipients.containsAll(recipients));

            // Match everything.
            return (sameCollapseKey && sameCredentials && samePriority && sameTtl &&
                    sameDelayWhileIdle && sameDryRun && samePayloadData && sameRecipients);
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += collapseKey != null
                ? collapseKey.hashCode()
                : hashCode;

        hashCode += credentials != null
                ? credentials.hashCode()
                : hashCode;

        hashCode += messagePriority != null
                ? messagePriority.hashCode()
                : hashCode;

        hashCode += ttlSeconds;

        hashCode += shouldDelayWhileIdle ? 1 : 0;

        hashCode += isDryRun ? 1 : 0;

        hashCode += payloadData != null
                ? payloadData.hashCode()
                : hashCode;

        hashCode += recipients != null
                ? recipients.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
