package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Finder;
import io.ebean.Model;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "locations", schema = "agency_alerts")
public class Location extends Model implements Comparable<Location> {
    public static Finder<Long, Location> find = new Finder<>(Location.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "location_id_seq_gen", sequenceName = "location_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_id_seq_gen")
    public Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "alert_id",
            table = "agency_alerts.alerts",
            referencedColumnName = "id")
    public Alert alert;

    @Column(name = "name")
    public String name;

    @Column(name = "latitude")
    public String latitude;

    @Column(name = "longitude")
    public String longitude;

    @Column(name = "message", columnDefinition = "TEXT")
    public String message;

    @Column(name = "sequence")
    public Integer sequence;

    @Basic
    @Column(name = "date", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar date;

    @Override
    public boolean equals(Object o) {
        if (o instanceof Location) {
            Location other = (Location) o;

            // Match on everything else.
            boolean sameName = StringUtils.equals(name, other.name);

            boolean sameMessage = StringUtils.equals(message, other.message);

            boolean sameSequence = sequence == null && other.sequence == null ||
                    (sequence != null && sequence.equals(other.sequence));

            boolean sameLatitude = StringUtils.equals(latitude, other.latitude);

            boolean sameLongitude = StringUtils.equals(longitude, other.longitude);

            // Match everything.
            return (sameName && sameMessage && sameSequence && sameLatitude && sameLongitude);
        }
        return o.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += message != null
                ? message.hashCode()
                : hashCode;

        hashCode += sequence != null
                ? sequence.hashCode()
                : hashCode;

        hashCode += latitude != null
                ? latitude.hashCode()
                : hashCode;

        hashCode += sequence != null
                ? sequence.hashCode()
                : hashCode;

        hashCode += date != null
                ? date.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(Location o) {
        return equals(o) ? -1 : 0;
    }
}
