package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import enums.AlertLevel;
import enums.AlertType;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model implements Comparable {
    public static Finder<Long, Alert> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Alert.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "alert_id_seq_gen", sequenceName = "alert_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_id_seq_gen")
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "route_id",
            referencedColumnName = "id",
            unique = true,
            updatable = false)
    public Route route;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public AlertType type;

    @Column(name = "level")
    @Enumerated(EnumType.STRING)
    public AlertLevel level;

    @Column(name = "message_title", columnDefinition = "TEXT")
    public String messageTitle;

    @Column(name = "message_subtitle", columnDefinition = "TEXT")
    public String messageSubtitle;

    @Column(name = "message_body", columnDefinition = "TEXT")
    public String messageBody;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    public String externalUri;

    @Nonnull
    @OneToMany(mappedBy = "alert", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public List<Location> locations = new ArrayList<>();

    @Basic
    @Column(name = "last_updated", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastUpdated;

    @Override
    public int hashCode() {
        int hashCode = type != null
                ? type.hashCode()
                : super.hashCode();

        hashCode += level != null
                ? level.hashCode()
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

        hashCode += lastUpdated != null
                ? lastUpdated.hashCode()
                : hashCode;

        hashCode += locations != null
                ? locations.hashCode()
                : hashCode;

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Alert) {
            Alert other = (Alert) obj;

            boolean sameType = (type == null && other.type == null) ||
                    (type != null && other.type != null && type.equals(other.type));

            boolean sameLevel = (level == null && other.level == null) ||
                    (level != null && other.level != null && level.equals(other.level));

            boolean sameTitle = (messageTitle == null && other.messageTitle == null) ||
                    (messageTitle != null && other.messageTitle != null && messageTitle.equals(other.messageTitle));

            boolean sameSubtitle = (messageSubtitle == null && other.messageSubtitle == null) ||
                    (messageSubtitle != null && other.messageSubtitle != null && messageSubtitle.equals(other.messageSubtitle));

            boolean sameBody = (messageBody == null && other.messageBody == null) ||
                    (messageBody != null && other.messageBody != null && messageBody.equals(other.messageBody));

            boolean sameUri = (externalUri == null && other.externalUri == null) ||
                    (externalUri != null && other.externalUri != null && externalUri.equals(other.externalUri));

            boolean sameUpdateTime = lastUpdated == null && other.lastUpdated == null ||
                    (lastUpdated != null && other.lastUpdated != null &&
                            lastUpdated.getTimeInMillis() == other.lastUpdated.getTimeInMillis());

            boolean bothLocationsNull = locations == null && other.locations == null;

            boolean sameLocations = bothLocationsNull || (locations != null && other.locations != null &&
                                locations.containsAll(other.locations) &&
                                other.locations.containsAll(locations));

            // Match everything.
            return (sameType && sameLevel && sameTitle && sameSubtitle &&
                    sameBody && sameUri && sameUpdateTime && sameLocations);
        }

        return obj.equals(this);
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
}
