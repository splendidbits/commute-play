package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import dispatcher.types.RecipientState;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Integer, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    @Id
    @Column(name = "recipient_id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    public Integer id;

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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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

    public Recipient() {

    }

    public Recipient(int id, String token) {
        this.id = id;
        this.token = token;
    }
}
