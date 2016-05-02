package models.taskqueue;

import com.avaje.ebean.Model;
import dispatcher.types.RecipientState;
import main.Constants;

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
    public RecipientState state;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "time_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeAdded;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "failure_id",
            referencedColumnName = "id",
            unique = true,
            updatable = false)
    public RecipientFailure failure;

    @PrePersist
    public void insertValues(){
        timeAdded = Calendar.getInstance();
        state = RecipientState.STATE_IDLE;
    }

    @PreUpdate
    public void updateValues() {
        timeAdded = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public Recipient() {
    }

    public Recipient(Long id, String token) {
        this.id = id;
        this.token = token;
    }
}
