package models.alerts;

import helpers.CompareUtils;
import io.ebean.Finder;
import io.ebean.Model;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "agencies", schema = "agency_alerts")
public class Agency extends Model implements Comparable<Agency> {
    public static Finder<String, Agency> find = new Finder<>(Agency.class);

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    private String externalUri;

    @Column(name = "utc_offset")
    private Float utcOffset;

    @OneToMany(mappedBy = "agency", orphanRemoval = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @JoinColumn(name = "agency_id", table = "agency_alerts.routes", referencedColumnName = "id")
    private List<Route> routes;

    public Agency(String id) {
        setId(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getExternalUri() {
        return externalUri;
    }

    public void setExternalUri(String externalUri) {
        this.externalUri = externalUri;
    }

    public Float getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(Float utcOffset) {
        this.utcOffset = utcOffset;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Agency) {
            Agency other = (Agency) obj;

            boolean sameId = CompareUtils.isEquals(id, other.id);

            boolean sameName = CompareUtils.isEquals(name, other.name);

            boolean samePhone = (CompareUtils.isEquals(phone, other.phone));

            boolean sameUri = CompareUtils.isEquals(externalUri, other.externalUri);

            boolean sameUtcOffset = CompareUtils.isEquals(utcOffset, other.utcOffset);

            boolean sameRoutes = CompareUtils.isEquals(routes, other.routes);

            // Match everything.
            return (sameId && sameName && samePhone && sameUri && sameUtcOffset && sameRoutes);
        }

        return obj.equals(this);
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += id != null
                ? id.hashCode()
                : hashCode;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += phone != null
                ? phone.hashCode()
                : hashCode;

        hashCode += externalUri != null
                ? externalUri.hashCode()
                : hashCode;

        hashCode += utcOffset != null
                ? utcOffset.hashCode()
                : hashCode;

        hashCode += routes != null
                ? routes.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public int compareTo(@NotNull Agency o) {
        return equals(o) ? -1 : 0;
    }
}
