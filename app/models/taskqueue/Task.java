package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;
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

    @Column("retry_multiplier");
    public String retryMultiplier;

    @Basic
    @Column(name = "first_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar firstAttempt;

    @Basic
    @Column(name = "last_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastAttempt;

    @Basic
    @Column(name = "next_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar nextAttempt;
}