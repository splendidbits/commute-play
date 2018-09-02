package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.PrivateOwned;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "locations", schema = "agency_alerts")
public class Location extends Model implements Comparable<Location> {
    public static Finder<Integer, Location> find = new Finder<>(Location.class);

    @Id
    @Column(name = "id")
    private Integer id;

    @JsonIgnore
    @PrivateOwned
    @ManyToOne
    private Alert alert;

    @Column(name = "name")
    private String name;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "sequence")
    private Integer sequence;

    @Basic
    @Column(name = "date", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar date;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Location) {
            Location other = (Location) o;

            // Match on everything else.
            boolean sameName = CompareUtils.isEquals(name, other.name);

            boolean sameMessage = CompareUtils.isEquals(message, other.message);

            boolean sameSequence = CompareUtils.isEquals(this.sequence, other.sequence);

            boolean sameLatitude = CompareUtils.isEquals(latitude, other.latitude);

            boolean sameLongitude = CompareUtils.isEquals(longitude, other.longitude);

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

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(Location o) {
        return equals(o) ? -1 : 0;
    }
}
