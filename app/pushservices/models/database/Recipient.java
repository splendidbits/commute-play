package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.enums.RecipientState;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Calendar;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "recipients", schema = "push_services")
public class Recipient extends Model implements Cloneable {
    public static Finder<Long, Recipient> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Recipient.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    protected Long id;

    @Column(name = "token", columnDefinition = "TEXT")
    public String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "push_services.messages",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Message message;

    @Column(name = "state")
    public RecipientState state = RecipientState.STATE_IDLE;

    @Basic(fetch=FetchType.LAZY)
    @Column(name = "time_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar timeAdded = Calendar.getInstance();

    @Column(name = "send_attempts")
    public int sendAttemptCount = 1;

    @OneToOne(mappedBy = "recipient", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public RecipientFailure failure;

    @Basic
    @Column(name = "previous_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar previousAttempt;

    @Basic
    @Column(name = "next_attempt")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar nextAttempt;

    @SuppressWarnings("unused")
    public Recipient() {
    }

    public Recipient(@Nonnull String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Recipient) {
            Recipient other = (Recipient) obj;

            boolean sameToken = (token == null && other.token == null) ||
                    (token != null && other.token != null && token.equals(other.token));

            boolean sameState = (state == null && other.state == null) ||
                    (state != null && other.state != null && state.equals(other.state));

            boolean sameFailure = (failure == null && other.failure == null) ||
                    (failure != null && other.failure != null && failure.equals(other.failure));

            // Match everything.
            return (sameToken && sameState && sameFailure);
        }
        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += token != null
                ? token.hashCode()
                : hashCode;

        hashCode += state != null
                ? state.hashCode()
                : hashCode;

        hashCode += failure != null
                ? failure.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
