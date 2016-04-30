package models.alerts;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "agencies", schema = "agency_alerts")
public class Agency extends Model {
    public static Finder<Integer, Agency> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Agency.class);

    @Id
    @Column(name = "agency_id")
    public Integer id;

    @Column(name = "agency_name")
    public String agencyName;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "agency", fetch = FetchType.LAZY)
    public List<Route> routes;
}
