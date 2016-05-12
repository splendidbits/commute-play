package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.enums.FailureType;

import javax.persistence.*;
import java.util.Calendar;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/6/16 Splendid Bits.
 */
@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "recipient_failures", schema = "push_services")
public class RecipientFailure extends Model implements Cloneable {
    public static Finder<Long, RecipientFailure> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, RecipientFailure.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "failure_id_seq_gen", sequenceName = "failure_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failure_id_seq_gen")
    protected Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "recipient_id",
            table = "push_services.recipients",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Recipient recipient;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public FailureType type;

    @Column(name = "message")
    public String message;

    @Basic
    @Column(name = "fail_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar failTime = Calendar.getInstance();

    @PrePersist
    public void updateValues() {
        failTime = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public RecipientFailure() {
    }

    public RecipientFailure(FailureType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecipientFailure) {
            RecipientFailure other = (RecipientFailure) obj;

            boolean sameType = (type == null && other.type == null) ||
                    (type != null && other.type != null && type.equals(other.type));

            boolean sameMessage = (message == null && other.message == null) ||
                    (message != null && other.message != null && message.equals(other.message));

            boolean sameRecipient = (recipient == null && other.recipient == null) ||
                    (recipient != null && other.recipient != null && recipient.equals(other.recipient));

            // Match everything.
            return (sameType && sameMessage && sameRecipient);
        }
        return obj.equals(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
