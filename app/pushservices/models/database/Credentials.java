package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;
import pushservices.enums.PlatformType;

import javax.persistence.*;


@Entity
@Table(name = "credentials", schema = "push_services")
public class Credentials extends Model {
    public static Finder<Long, Credentials> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Credentials.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "credentials_id_seq_gen", sequenceName = "credentials_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credentials_id_seq_gen")
    public Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "push_services.messages",
            referencedColumnName = "id",
            nullable = true,
            unique = false,
            insertable = true,
            updatable = true)
    public Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    public PlatformType platformType;

    @Column(name = "authorisation_key", columnDefinition = "TEXT")
    public String authorisationKey;

    @Column(name = "certificate_body", columnDefinition = "TEXT")
    public String certificateBody;

    @Column(name = "package_uri", columnDefinition = "TEXT")
    public String packageUri;

    public Credentials() {

    }
}
