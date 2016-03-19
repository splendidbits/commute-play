package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Agency;
import models.alerts.Alert;
import models.alerts.Route;
import models.registrations.Registration;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "message", schema = "service_accounts")
public class Message extends Model {
    public static Finder<Integer, Message> find = new Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, Message.class);

    public Message() {
    }

    @Id
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    @Column(name = "message_id")
    public Integer messageId;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @Column(name = "route_id")
    public Route route;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    @Column(name = "account_id")
    public Account account;

    @Column(name = "delivered_recipients")
    public Integer deliveredRecipients;

    @Column(name = "undelivered_recipients")
    public Integer undeliveredRecipients;

    @Column(name = "failed_recipients")
    public Integer failedRecipients;

    @Basic
    @Column(name = "sent_time")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar sentTime = Calendar.getInstance();
}
