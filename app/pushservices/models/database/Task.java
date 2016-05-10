package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.types.TaskState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "tasks", schema = "push_services")
public class Task extends Model {
    public static Finder<Long, Task> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Task.class);

    // While priority can be set to any int, these might be useful. Bigger int is higher priority.
    public static final int TASK_PRIORITY_HIGH = 10;
    public static final int TASK_PRIORITY_MEDIUM = 5;
    public static final int TASK_PRIORITY_LOW = 1;

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Long id;

    @Nullable
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "name")
    public String name;

    @Column(name = "state")
    public TaskState state = TaskState.STATE_IDLE;

    @Column(name = "priority")
    public int priority = TASK_PRIORITY_LOW;

    @Column(name = "retry_count")
    public int retryCount;

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

    public Task(String name) {
        Calendar nowTime = Calendar.getInstance();
        this.name = name;
        this.taskAdded = nowTime;
        this.nextAttempt = nowTime;
        this.retryCount = 0;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += messages != null
                ? messages.hashCode()
                : hashCode;

        hashCode += state != null
                ? state.hashCode()
                : hashCode;

        hashCode += priority;

        return hashCode.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task) {
            Task other = (Task) obj;

            boolean sameTaskName = name == null && other.name == null ||
                    name != null && name.equals(other.name);

            boolean samePriority = priority == other.priority;

            boolean bothMessagesEmpty = messages == null && other.messages == null;

            boolean sameMessages = bothMessagesEmpty || (messages != null && other.messages != null &&
                    messages.containsAll(other.messages) && other.messages.containsAll(messages));

            return sameTaskName && sameMessages && samePriority;
        }
        return obj.equals(this);
    }
}