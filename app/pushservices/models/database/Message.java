package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;
import pushservices.enums.MessagePriority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "messages", schema = "push_services")
public class Message extends Model {
    public static Finder<Long, Message> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Message.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(
            name = "task_id",
            table = "push_services.tasks",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Task task;

    @OneToOne(mappedBy = "message", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public Credentials credentials;

    @Nullable
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @Nullable
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    public MessagePriority messagePriority;

    @Column(name = "time_to_live")
    public int ttl;

    @Column(name = "delay_while_idle")
    public boolean shouldDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Basic
    @Column(name = "sent_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime;

    @Transient
    public void addRecipient(@Nonnull Recipient recipient) {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        recipients.add(recipient);
    }

    @PrePersist
    public void initialValues() {
        sentTime = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public Message() {
        messagePriority = MessagePriority.PRIORITY_NORMAL;
        ttl = 86400;
    }
}
