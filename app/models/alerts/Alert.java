package models.alerts;

import com.avaje.ebean.Model;
import models.registrations.Subscription;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "alerts", schema = "public")
public class Alert extends Model{
    public static Finder<Integer, Alert> find = new Model.Finder<>("route_alerts", Integer.class, Alert.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name="alerts_id_seq_gen", sequenceName="public.alerts_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.alerts_id_seq_gen")
	public Integer alertId;

    @ManyToOne(fetch=FetchType.LAZY)
    public Route route;

    @ManyToMany
    @JoinTable(name="alert_subscription",
            joinColumns=@JoinColumn(name="alert_id",referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name="subscription_id", referencedColumnName="id"))
    public List<Subscription> subscriptions;

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
