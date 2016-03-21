package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "recipients", schema = "task_queue")
public class Recipient extends Model {
    public static Finder<Integer, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    public Recipient() {
        isRunning = false;
    }

    @Id
    @Column("recipient_id")
    public String recipientId;

    @ManyToOne(fetch = FetchType.EAGER)
    public Message message;

    @Column("is_running")
    public boolean isRunning;
}
