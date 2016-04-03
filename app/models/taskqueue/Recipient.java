package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.avaje.ebean.annotation.EnumValue;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Integer, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    @Id
    @Column(name = "recipient_id")
    public String recipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    public Message message;

    @Basic
    @Column(name = "last_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastAttempt;

    @PrePersist
    @PreUpdate
    public void initialValues() {
        lastAttempt = Calendar.getInstance();
    }
}
