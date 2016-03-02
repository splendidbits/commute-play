package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import helpers.CompareUtils;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model implements Comparable {
    public static Finder<Integer, Alert> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Alert.class);

    @Id
    @Column(name = "alert_id", updatable = true)
    @SequenceGenerator(name = "alerts_id_seq_gen", sequenceName = "alerts_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alerts_id_seq_gen")
    public Integer alertId;

    @ManyToOne(fetch = FetchType.EAGER)
    public Route route;

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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Alert) {
            Alert other = (Alert) obj;

            // Match potential routeIds first.
            boolean bothRouteIdMatch = (other.route != null && route != null) && other.route.routeId.equals(route.routeId);

            // Match on everything else.
            boolean sameCurrentMessage = currentMessage.equals(other.currentMessage);
            boolean sameDetourMessage = detourMessage != null && detourMessage.equals(other.detourMessage);
            boolean sameAdvisoryMessage = advisoryMessage != null && advisoryMessage.equals(other.advisoryMessage);
            boolean sameDetourReason = detourReason != null && detourReason.equals(other.detourReason);
            boolean sameDetourStartLocation = detourStartLocation != null && detourStartLocation.equals(other.detourStartLocation);
            boolean sameSnow = isSnow == other.isSnow;

            boolean sameDetourStartDate = CompareUtils.equalsNullSafe(detourStartDate, other.detourStartDate);
            boolean sameDetourEndDate =  CompareUtils.equalsNullSafe(detourEndDate, other.detourEndDate);

            // If there was no possible routeId match, return false.
            if (!bothRouteIdMatch) {
                return false;
            }

            // Match everything else.
            return (sameCurrentMessage && sameDetourMessage &&
                    sameAdvisoryMessage && sameDetourReason && sameDetourStartLocation &&
                    sameDetourStartDate && sameDetourEndDate && sameSnow);
        }
        return obj.equals(this);
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Alert) {
            Alert other = (Alert) o;
            if (lastUpdated.before(other.lastUpdated)) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }
}
