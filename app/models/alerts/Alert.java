package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.AlertType;
import main.Constants;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "alerts", schema = "agency_updates")
public class Alert extends Model implements Comparable {
    public static Finder<Long, Alert> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Alert.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "alert_id_seq_gen", sequenceName = "alert_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_id_seq_gen")
    public Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "route_id",
            table = "agency_updates.routes",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
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

            boolean sameUpdateTime = lastUpdated == null && other.lastUpdated == null ||
                    (lastUpdated != null && other.lastUpdated != null &&
                            lastUpdated.getTimeInMillis() == other.lastUpdated.getTimeInMillis());

            boolean bothLocationsEmpty = locations == null && other.locations == null ||
                    (locations != null && locations.isEmpty() && other.locations != null && other.locations.isEmpty());

            boolean sameLocations = bothLocationsEmpty || locations != null && other.locations != null &&
                            (locations.containsAll(other.locations) && other.locations.containsAll(locations));

            // Match everything.
            return (sameType && sameTitle && sameSubtitle && sameBody && sameUri && sameUpdateTime && sameLocations);
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        id = null;
        markPropertyUnset("id");
        return super.clone();
    }
}
