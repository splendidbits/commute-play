package models.alerts;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model {
    public static Finder<Integer, Alert> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Alert.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "alerts_id_seq_gen", sequenceName = "alerts_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alerts_id_seq_gen")
    public Integer alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    public Route route;

    @Transient
    public String routeId;

    @Column(name = "current_message", columnDefinition = "TEXT")
    public String currentMessage;

    @Column(name = "advisory_message", columnDefinition = "TEXT")
    public String advisoryMessage;

    @Column(name = "detour_message", columnDefinition = "TEXT")
    public String detourMessage;

    @Column(name = "detour_start_location", columnDefinition = "TEXT")
    public String detourStartLocation;

    @Basic
    @Column(name = "detour_start_date")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar detourStartDate;

    @Basic
    @Column(name = "detour_end_date")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar detourEndDate;

    @Column(name = "detour_reason", columnDefinition = "TEXT")
    public String detourReason;

    @Column(name = "is_snow")
    public boolean isSnow;

    @Basic
    @Column(name = "last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated;
}
