package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.helpers.PlatformMessageBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "tasks", schema = "push_services")
public class Task extends Model implements Cloneable {
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

    @Column(name = "priority")
    public int priority = TASK_PRIORITY_LOW;

    @Basic
    @Column(name = "last_updated", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated = Calendar.getInstance();

    /**
     * Add a platform message to the task. Ensure that the message contains the
     * credentials attribute. Many different push services / accounts can be used per task.
     *
     * @param platformMessage The message to add. Use {@link PlatformMessageBuilder}
     *                        to easily create a platform message.
     */
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
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task) {
            Task other = (Task) obj;

            boolean sameTaskName = name == null && other.name == null ||
                    name != null && name.equals(other.name);

            boolean samePriority = priority == other.priority;

            boolean bothMessagesEmpty = messages == null && other.messages == null ||
                    messages == null && other.messages.isEmpty() ||
                    messages.isEmpty() && other.messages == null;

            boolean sameMessages = bothMessagesEmpty || messages != other.messages ||
                    (messages.containsAll(other.messages) && other.messages.containsAll(messages));

            return sameTaskName && samePriority && sameMessages;
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += priority;

        hashCode += messages != null
                ? messages.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

        @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}