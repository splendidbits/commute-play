package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "agencies", schema = "agency_updates")
public class Agency extends Model {
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
}
