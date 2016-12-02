package models.alerts;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "locations", schema = "agency_alerts")
public class Location extends Model implements Comparable {
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
            boolean sameName = name == null && other.name == null ||
                    (name != null && name.equals(other.name));

            boolean sameMessage = message == null && other.message == null ||
                    (message != null && message.equals(other.message));

            boolean sameSequence = sequence == null && other.sequence == null ||
                    (sequence != null && sequence.equals(other.sequence));

            boolean sameLatitude = latitude == null && other.latitude == null ||
                    (latitude != null && latitude.equals(other.latitude));

            boolean sameLongitude = longitude == null && other.longitude == null ||
                    (longitude != null && longitude.equals(other.longitude));

            boolean sameDate = date == null && other.date == null ||
                    (date != null && other.date != null &&
                            date.getTimeInMillis() == other.date.getTimeInMillis());

            // Match everything.
            return (sameName && sameMessage && sameLatitude && sameLongitude &&
                    sameSequence && sameDate);
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

        hashCode += latitude != null
                ? latitude.hashCode()
                : hashCode;

        hashCode += longitude != null
                ? longitude.hashCode()
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
    public int compareTo(Object o) {
        if (o instanceof Location) {
            Location other = (Location) o;
            if (name != null) {
                return  name.equals(other.name) ? -1 : 0;
            }
        }
        return 0;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        markAsDirty();

        Location location = new Location();
        location.id = id;
        location.name = name;
        location.message = message;
        location.latitude = latitude;
        location.longitude = longitude;
        location.sequence = sequence;
        location.date = date;
        location.alert = alert;

        return location;
    }
}
