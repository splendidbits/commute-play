package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.avaje.ebean.annotation.EnumValue;
import main.Constants;

import javax.persistence.*;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Integer, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    public enum ProcessState {
        @EnumValue("NOT_STARTED")
        STATE_NOT_STARTED,
        @EnumValue("PROCESSING")
        STATE_PROCESSING,
        @EnumValue("ERROR_WAITING_FOR_RETRY")
        STATE_ERROR_WAITING_FOR_RETRY,
        @EnumValue("COMPLETE")
        STATE_COMPLETE,
        @EnumValue("PERMINANTLY_FAILED")
        STATE_PERMINANTLY_FAILED
    }

    @Id
    @Column(name = "recipient_id")
    public String recipientId;

    @ManyToOne(fetch = FetchType.EAGER)
    public Message message;

    @Column(name = "process_state")
    public ProcessState processState;

    public Recipient() {
        processState = ProcessState.STATE_NOT_STARTED;
    }

}
