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
    public static Finder<Integer, Agency> find = new Finder<>(Agency.class);

    @Id
    @Column(name = "id")
    public Integer id;

    @Column(name = "name")
    public String name;

    @Column(name = "phone")
    public String phone;

    @Column(name = "external_uri", columnDefinition = "TEXT")
    public String externalUri;

    @Column(name = "utc_offset")
    public Float utcOffset;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "agency", fetch = FetchType.EAGER)
    public List<Route> routes;

    @SuppressWarnings("unused")
    private Agency() {
    }

    public Agency(int id) {
        this.id = id;
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
