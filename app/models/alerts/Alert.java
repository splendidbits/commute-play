package models.alerts;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "alerts", schema = "public")
public class Alert extends Model{
    public static Finder<Integer, Alert> find = new Model.Finder<>("route_alerts", Integer.class, Alert.class);

    @Id
    @SequenceGenerator(name="alerts_id_seq_gen", sequenceName="public.alerts_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.alerts_id_seq_gen")
	public Integer id;

    @Column(name="route_id")
    public String routeId;

    @ManyToOne(fetch=FetchType.LAZY)
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
    @Column(name="detour_start_date")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar detourStartDate;

    @Basic
    @Column(name="detour_end_date")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar detourEndDate;

    @Column(name="detour_reason", columnDefinition = "TEXT")
    public String detourReason;

    @Column(name="is_snow")
    public boolean isSnow;

    @Basic
    @Column(name="last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated;
}
