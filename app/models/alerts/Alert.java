package models.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.AlertType;
import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.PrivateOwned;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "alerts", schema = "agency_alerts")
public class Alert extends Model implements Comparable<Alert> {
    public static Finder<Integer, Alert> find = new Finder<>(Alert.class);

    @Id
    @Column(name = "id")
    private Integer id;

    @OneToMany(mappedBy = "alert", orphanRemoval = true, cascade = CascadeType.MERGE)
//    @OneToMany(mappedBy = "alert", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @JoinColumn(name = "alert_id", table = "agency_alerts.locations", referencedColumnName = "id")
    private List<Location> locations;

    @JsonIgnore
    @PrivateOwned
    @ManyToOne
    private Route route;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(name = "message_title", columnDefinition = "text")
    private String messageTitle;

    @Column(name = "message_subtitle", columnDefinition = "text")
    private String messageSubtitle;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    @Column(name = "external_uri", columnDefinition = "text")
    private String externalUri;

    @Column(name = "high_priority", columnDefinition = "boolean default false")
    private Boolean highPriority;

    @Basic
    @Column(name = "last_updated", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar lastUpdated;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    public String getMessageSubtitle() {
        return messageSubtitle;
    }

    public void setMessageSubtitle(String messageSubtitle) {
        this.messageSubtitle = messageSubtitle;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getExternalUri() {
        return externalUri;
    }

    public void setExternalUri(String externalUri) {
        this.externalUri = externalUri;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public Boolean getHighPriority() {
        return highPriority;
    }

    public void setHighPriority(Boolean highPriority) {
        this.highPriority = highPriority;
    }

    public Calendar getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Calendar lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

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

            boolean samePriority = CompareUtils.isEquals(highPriority, other.highPriority);

            boolean sameLastUpdated = CompareUtils.isEquals(lastUpdated, other.lastUpdated);

            // Match everything.
            return (sameType && sameTitle && sameSubtitle && sameBody && sameLocations && sameExternalUri && samePriority & sameLastUpdated);
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

        hashCode += lastUpdated != null
                ? lastUpdated.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(@NotNull Alert o) {
        return equals(o) ? -1 : 0;
    }
}
