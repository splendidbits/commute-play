package models.alerts;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.AlertType;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model implements Comparable {
    public static Finder<Long, Alert> find = new Finder<>(Alert.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "alert_id_seq_gen", sequenceName = "alert_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_id_seq_gen")
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "route_id",
            table = "agency_alerts.routes",
            referencedColumnName = "id")
    public Route route;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public AlertType type = AlertType.TYPE_NONE;

    @Column(name = "message_title", columnDefinition = "TEXT")
    public String messageTitle;

    @Column(name = "message_subtitle", columnDefinition = "TEXT")
    public String messageSubtitle;

    @Column(name = "message_body", columnDefinition = "TEXT")
    public String messageBody;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    public String externalUri;

    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public List<Location> locations;

    @Column(name  = "high_priority", nullable = false)
    public Boolean highPriority = false;

    @Basic
    @Column(name = "last_updated", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Alert) {
            Alert other = (Alert) obj;

            boolean sameType = (type.equals(other.type));

            boolean sameTitle = (messageTitle == null && other.messageTitle == null) ||
                    (messageTitle != null && other.messageTitle != null && messageTitle.equals(other.messageTitle));

            boolean sameSubtitle = (messageSubtitle == null && other.messageSubtitle == null) ||
                    (messageSubtitle != null && other.messageSubtitle != null && messageSubtitle.equals(other.messageSubtitle));

            boolean sameBody = (messageBody == null && other.messageBody == null) ||
                    (messageBody != null && other.messageBody != null && messageBody.equals(other.messageBody));

            boolean sameUri = (externalUri == null && other.externalUri == null) ||
                    (externalUri != null && other.externalUri != null && externalUri.equals(other.externalUri));

            boolean samePriority = other.highPriority == highPriority;

            boolean bothLocationsEmpty = locations == null && other.locations == null ||
                    (locations != null && locations.isEmpty() && other.locations != null && other.locations.isEmpty());

            boolean sameLocations = bothLocationsEmpty || locations != null && other.locations != null &&
                            (locations.containsAll(other.locations) && other.locations.containsAll(locations));

            // Match everything.
            return (sameType && sameTitle && sameSubtitle && sameBody && sameUri && sameLocations && samePriority);
        }

        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += type != null
                ? type.hashCode()
                : hashCode;

        hashCode += messageTitle != null
                ? messageTitle.hashCode()
                : hashCode;

        hashCode += messageSubtitle != null
                ? messageSubtitle.hashCode()
                : hashCode;

        hashCode += messageBody != null
                ? messageBody.hashCode()
                : hashCode;

        hashCode += externalUri != null
                ? externalUri.hashCode()
                : hashCode;

        hashCode += locations != null
                ? locations.hashCode()
                : hashCode;

        hashCode += lastUpdated != null
                ? lastUpdated.hashCode()
                : hashCode;

        hashCode += highPriority ? 1 : 0;

        return hashCode.hashCode();
    }

    @SuppressWarnings("unused")
    public Alert() {
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Alert) {
            Alert other = (Alert) o;
            if (messageBody != null) {
                return messageBody.equals(other.messageBody) ? -1 : 0;
            }
        }
        return 0;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Alert alert = new Alert();
        alert.id = id;
        alert.route = route;
        alert.type = type;
        alert.messageTitle = messageTitle;
        alert.messageSubtitle = messageSubtitle;
        alert.messageBody = messageBody;
        alert.externalUri = externalUri;
        alert.locations = locations;
        alert.lastUpdated = lastUpdated;
        alert.highPriority = highPriority;
        alert.markAsDirty();
        return alert;
    }
}
