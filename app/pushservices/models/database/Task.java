package pushservices.models.database;

import com.avaje.ebean.Model;
import pushservices.types.TaskState;
import main.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "tasks", schema = "task_queue")
public class Task extends Model {
    public static Finder<Long, Task> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Task.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Long id;

    @Nullable
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "retry_count")
    public int retryCount;

    @Column(name = "name")
    public String name;

    @Column(name = "state")
    public TaskState state;

    @Basic
    @Column(name = "task_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar taskAdded;

    @Basic
    @Column(name = "last_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastAttempt;

    @Basic
    @Column(name = "next_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar nextAttempt;

    public void addMessage(@Nonnull Message platformMessage) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(platformMessage);
    }

    @SuppressWarnings("unused")
    public Task() {
    }

    public Task(@Nonnull String name) {
        this.name = name;
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