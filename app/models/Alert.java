package models;

import com.avaje.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "alerts_table")
public class Alert extends Model{
    public static Finder<Integer, Alert> find = new Model.Finder<>(Integer.class, Alert.class);

    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Id
	public Integer id;

    @ManyToOne
    public Route route;

    public String currentMessage;

    public String advisoryMessage;

    public String detourMessage;

    public String detourStartLocation;

    public String detourStartDate;

    public String detourEndDate;

    public String detourReason;

    public boolean isSnow;

    public String lastUpdated;
}
