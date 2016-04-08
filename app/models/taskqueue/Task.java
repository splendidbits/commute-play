package models.taskqueue;

import com.avaje.ebean.Model;
import main.Constants;
import models.app.ProcessState;

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
    public Integer taskId;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "task", fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "retry_count")
    public int retryCount;

    @Column(name = "process_state")
    public ProcessState processState;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "task_added")
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

        processState = ProcessState.STATE_IDLE;
        taskAdded = nowTime;
        nextAttempt = nowTime;
        retryCount = 0;
    }
}