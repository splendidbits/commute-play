package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "agencies", schema = "agency_updates")
public class Agency extends Model implements Cloneable {
    public static Finder<Integer, Agency> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Agency.class);

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
                    (routes != null && routes.isEmpty() && other.routes != null && other.routes.isEmpty());

            boolean sameRoutes = bothRoutesEmpty || routes != null && other.routes != null &&
                    (routes.containsAll(other.routes) && other.routes.containsAll(routes));

            // Match everything.
            return (sameId && sameName && samePhone && sameUri && sameUtcOffset && sameRoutes);
        }

        return obj.equals(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        id = null;
        markPropertyUnset("id");
        return super.clone();
    }
}
