package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "locations", schema = "agency_updates")
public class Location extends Model implements Comparable {
    public static Finder<Long, Location> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Location.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "location_id_seq_gen", sequenceName = "location_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_id_seq_gen")
    public Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "alert_id",
            table = "agency_updates.alerts",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
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
    public int hashCode() {
        int hashCode = name != null
                ? name.hashCode()
                : super.hashCode();

        hashCode += message != null
                ? message.hashCode()
                : hashCode;

        hashCode += sequence != null
                ? sequence.hashCode()
                : hashCode;

        hashCode += latitude != null
                ? latitude.hashCode()
                : hashCode;

        hashCode += longitude != null
                ? longitude.hashCode()
                : hashCode;

        hashCode += date != null
                ? date.hashCode()
                : hashCode;

        return hashCode;
    }

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
    public int compareTo(Object o) {
        if (o instanceof Location) {
            Location other = (Location) o;
            if (name != null) {
                return  name.equals(other.name) ? -1 : 0;
            }
        }
        return 0;
    }
}
