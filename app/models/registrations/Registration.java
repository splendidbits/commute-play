package models.registrations;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "registrations")
public class Registration extends Model {
    public static Finder<String, Registration> find = new Model.Finder<>(String.class, Registration.class);

    @Id
	public String devUuid;

    public String registrationId;

    public String timeRegistered;

    @OneToMany(mappedBy = "registration")
    public List<Subscription> subscriptions;
}
