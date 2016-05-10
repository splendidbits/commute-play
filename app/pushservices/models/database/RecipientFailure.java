package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;
import pushservices.enums.PlatformFailureType;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Calendar;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/6/16 Splendid Bits.
 */
@Entity
@Table(name = "recipient_failures", schema = "push_services")
public class RecipientFailure extends Model {
    public static Finder<Long, RecipientFailure> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, RecipientFailure.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "failure_id_seq_gen", sequenceName = "failure_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failure_id_seq_gen")
    public Long id;

    @Column(name = "failure")
    @Enumerated(EnumType.STRING)
    public PlatformFailureType failure;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "recipient_id",
            table = "push_services.recipients",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Recipient recipient;

    @Basic
    @Column(name = "fail_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar failTime;

    @PrePersist
    public void updateValues() {
        failTime = Calendar.getInstance();
    }

    @SuppressWarnings("unused")
    public RecipientFailure() {
    }

    public RecipientFailure(@Nonnull PlatformFailureType failure) {
        this.failure = failure;
    }
}
