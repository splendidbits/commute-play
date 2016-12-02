package models.alerts;

import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "agencies", schema = "agency_alerts")
public class Agency extends Model implements Cloneable {
    public static Finder<Integer, Agency> find = new Model.Finder<>(Agency.class);

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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "agency", fetch = FetchType.LAZY)
    public List<Route> routes;

    @SuppressWarnings("unused")
    public Agency() {
    }

    public Agency(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Agency) {
            Agency other = (Agency) obj;

            boolean sameId = (id == null && other.id == null) ||
                    (id != null && other.id != null && id.equals(other.id));

            boolean sameName = (name == null && other.name == null) ||
                    (name != null && other.name != null && name.equals(other.name));

            boolean samePhone = (phone == null && other.phone == null) ||
                    (phone != null && other.phone != null && phone.equals(other.phone));

            boolean sameUri = (externalUri == null && other.externalUri == null) ||
                    (externalUri != null && other.externalUri != null && externalUri.equals(other.externalUri));

            boolean sameUtcOffset = (utcOffset == null && other.utcOffset == null) ||
                    (utcOffset != null && other.utcOffset != null && utcOffset.equals(other.utcOffset));

            boolean bothRoutesEmpty = routes == null && other.routes == null ||
                    routes == null && other.routes.isEmpty() ||
                    routes.isEmpty() && other.routes == null;

            boolean sameRoutes = bothRoutesEmpty ||
                    routes != null && routes != null && other.routes != null &&
                            (routes.containsAll(other.routes) && other.routes.containsAll(routes));

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
    public Object clone() throws CloneNotSupportedException {
        markAsDirty();

        Agency agency = new Agency();
        agency.id = id;
        agency.name = name;
        agency.phone = phone;
        agency.externalUri = externalUri;
        agency.utcOffset = utcOffset;
        agency.routes = routes;

        return agency;
    }
}
