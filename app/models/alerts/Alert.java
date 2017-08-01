package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.AlertType;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model implements Comparable<Alert>, Cloneable {
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

    @Column(name = "high_priority", nullable = false)
    public Boolean highPriority = false;

    @Basic
    @Column(name = "last_updated", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Alert) {
            Alert other = (Alert) obj;

            boolean sameType = CompareUtils.isEquals(type, other.type);

            boolean sameTitle = CompareUtils.isEquals(messageTitle, other.messageTitle);

            boolean sameSubtitle = CompareUtils.isEquals(messageSubtitle, other.messageSubtitle);

            boolean sameBody = CompareUtils.isEquals(messageBody, other.messageBody);

            boolean sameLocations = CompareUtils.isEquals(locations, other.locations);

            boolean sameExternalUri = CompareUtils.isEquals(externalUri, other.externalUri);

            boolean samePriority = highPriority == other.highPriority;

            // Match everything.
            return (sameType && sameTitle && sameSubtitle && sameBody && sameLocations && sameExternalUri && samePriority);
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

        hashCode += locations != null
                ? locations.hashCode()
                : hashCode;

        hashCode += externalUri != null
                ? externalUri.hashCode()
                : hashCode;

        hashCode += highPriority != null
                ? highPriority.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(@NotNull Alert o) {
        return equals(o) ? -1 : 0;
    }

    @Override
    public Alert clone() throws CloneNotSupportedException {
        return (Alert) super.clone();
    }
}
