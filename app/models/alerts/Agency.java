package models.alerts;

import com.avaje.ebean.Model;
import jdk.nashorn.internal.ir.annotations.Reference;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "agencies", schema = "public")
public class Agency extends Model {
    public static Finder<Integer, Agency> find = new Model.Finder<>("route_alerts", Integer.class, Agency.class);

    @Id
    @Column(name = "id")
    public Integer agencyId;

    @Column(name = "agency_name")
    public String agencyName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "agency")
    public List<Route> routes;
}
