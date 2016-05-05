package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "credentials", schema = "task_queue")
public class Credentials extends Model {
    public static Finder<Long, Credentials> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Credentials.class);

    @Id
    @Column(name = "authorization_key")
    public String authorizationKey;

    @OneToMany(mappedBy="credentials", fetch = FetchType.EAGER)
    private List<Message> messages;

    @Column(name = "package_uri")
    public String packageUri;

    @Column(name = "certificate_body", columnDefinition = "TEXT")
    public String certificateBody;

    @Column(name = "endpoint_url")
    public String endpointUrl;

    @Column(name = "restricted_package_name")
    public String restrictedPackage;
}
