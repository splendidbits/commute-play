package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.avaje.ebean.annotation.EnumValue;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "tasks", schema = "task_queue")
public class Task extends Model {
    public static Finder<Integer, Task> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Task.class);

    public enum TaskProcessState {
        @EnumValue("NOT_STARTED")
        STATE_NOT_STARTED,
        @EnumValue("PROCESSING")
        STATE_PROCESSING,
        @EnumValue("PARTIALLY_COMPLETE")
        STATE_PARTIALLY_COMPLETE,
        @EnumValue("PERMANENTLY_FAILED")
        STATE_PERMANENTLY_FAILED,
        @EnumValue("COMPLETE")
        STATE_COMPLETE
    }

    @Id
    @Column(name = "task_id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Integer taskId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "exponential_multiplier")
    public int currentExponent;

    @Column(name = "process_state")
    public TaskProcessState processState;

    @Basic
    @Column(name = "task_added")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar taskAdded;

    @Basic
    @Column(name = "previous_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar previousAttempt;

    @Basic
    @Column(name = "upcoming_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar upcomingAttempt;

    public Task() {
        processState = TaskProcessState.STATE_NOT_STARTED;
    }

    public void addMessage(@Nonnull Message platformMessage) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(platformMessage);
    }

    @PrePersist
    public void initialValues() {
        taskAdded = Calendar.getInstance();
        currentExponent = 1;
    }
}