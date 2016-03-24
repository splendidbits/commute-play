package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import interfaces.PlatformMessage;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "tasks", schema = "task_queue")
public class Task extends Model {
    public static Finder<Integer, Task> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Task.class);

    @Id
    @Column(name = "task_id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Integer taskId;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "exponential_multiplier")
    public int currentExponent;

    @Column(name = "in_process")
    public boolean inProcess;

    @Basic(optional = false)
    @Column(name = "task_added")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar taskAdded;

    @Basic()
    @Column(name = "initial_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar initialAttempt;

    @Basic
    @Column(name = "previous_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar previousAttempt;

    @Basic
    @Column(name = "upcoming_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar upcomingAttempt;

    public void addMessage(@Nonnull Message platformMessage) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(platformMessage);
    }

    @Override
    public void insert() {
        initialAttempt = Calendar.getInstance();
        super.insert();
    }
}