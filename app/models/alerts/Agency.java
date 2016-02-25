package models.alerts;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "agencies", schema = "agency_alerts")
public class Agency extends Model {
    public static Finder<Integer, Agency> find = new Model.Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Agency.class);

    @Id
    @Column(name = "id")
    public Integer agencyId;

    @Column(name = "agency_name")
    public String agencyName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "agency")
    public List<Route> routes;
}
