package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.EnumValue;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Integer, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    public enum ProcessState {
        @EnumValue("IDLE")
        IDLE,
        @EnumValue("PROCESSING")
        PROCESSING,
        @EnumValue("RETRY")
        RETRY,
        @EnumValue("COMPLETE")
        COMPLETE
    }

    @Id
    @Column(name = "recipient_id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    public Integer id;

    @Column(name = "token")
    public String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="message_id")
    public Message message;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    public ProcessState state;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "time_added")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeAdded;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public RecipientFailure mRecipientFailure;

    @PrePersist
    public void insertValues(){
        timeAdded = Calendar.getInstance();
        state = ProcessState.IDLE;
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
