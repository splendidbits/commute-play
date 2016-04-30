package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import dispatcher.types.TaskState;
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

    @Id
    @Column(name = "task_id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Long id;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "retry_count")
    public int retryCount;

    @Column(name = "name")
    public String name;

    @Column(name = "state")
    public TaskState state;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "task_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar taskAdded;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "last_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastAttempt;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "next_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar nextAttempt;

    public Task() {
    }

    public Task(String name) {
        this.name = name;
    }

    @Transient
    public void addMessage(@Nonnull Message platformMessage) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(platformMessage);
    }

    @PrePersist
    public void initialValues() {
        Calendar nowTime = Calendar.getInstance();

        state = TaskState.STATE_IDLE;
        taskAdded = nowTime;
        nextAttempt = nowTime;
        retryCount = 0;
    }
}