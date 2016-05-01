package models.taskqueue;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/6/16 Splendid Bits.
 */
@Entity
@Table(name = "recipient_failures", schema = "task_queue")
public class RecipientFailure extends Model {
    public static Finder<Long, RecipientFailure> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, RecipientFailure.class);

    @Id
    @Column(name = "recipient_failure_id")
    @SequenceGenerator(name = "failure_id_seq_gen", sequenceName = "failure_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failure_id_seq_gen")
    public Long id;

    @OneToMany(mappedBy = "failure", fetch = FetchType.LAZY)
    public List<Recipient> recipients;

    @Column(name = "failure_reason")
    public String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    public Recipient recipient;

    @Basic
    @Column(name = "fail_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar failTime;

    @PrePersist
    public void updateValues() {
        failTime = Calendar.getInstance();
    }

    public RecipientFailure() {

    }

    public RecipientFailure(String failureReason) {
        this.failureReason = failureReason;
    }
}
