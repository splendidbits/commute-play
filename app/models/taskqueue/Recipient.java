package models.taskqueue;

import com.avaje.ebean.Model;
import dispatcher.types.RecipientState;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Long, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    public Long id;

    @Column(name = "token")
    public String token;

    @ManyToOne(fetch = FetchType.LAZY)
    public Message message;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    public RecipientState state = RecipientState.STATE_IDLE;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "time_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeAdded = Calendar.getInstance();

    @OneToOne(fetch = FetchType.EAGER)
    public RecipientFailure failure;

    @PreUpdate
    public void updateValues() {
        timeAdded = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public Recipient() {
    }

    public Recipient(@Nonnull Long id, @Nonnull String token) {
        this.id = id;
        this.token = token;
    }
}
