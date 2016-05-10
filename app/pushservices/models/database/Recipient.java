package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;
import pushservices.types.RecipientState;
import pushservices.types.TaskState;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "recipients", schema = "push_services")
public class Recipient extends Model {
    public static Finder<Long, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    public Long id;

    @Column(name = "token", columnDefinition = "TEXT")
    public String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "push_services.messages",
            referencedColumnName = "id",
            unique = true,
            updatable = true)
    public Message message;

//    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    public RecipientState state;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "time_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeAdded = Calendar.getInstance();

    @OneToOne(mappedBy = "recipient", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public RecipientFailure failure;

    @PreUpdate
    public void updateValues() {
        timeAdded = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public Recipient() {
    }

    public Recipient(@Nonnull String token) {
        this.token = token;
    }
}
