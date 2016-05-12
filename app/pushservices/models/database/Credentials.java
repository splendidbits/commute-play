package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;
import pushservices.enums.PlatformType;

import javax.persistence.*;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "credentials", schema = "push_services")
public class Credentials extends Model implements Cloneable {
    public static Finder<Long, Credentials> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Credentials.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "credentials_id_seq_gen", sequenceName = "credentials_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credentials_id_seq_gen")
    protected Long id;

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

    @SuppressWarnings("unused")
    public Credentials() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Credentials) {
            Credentials other = (Credentials) obj;

            boolean samePlatform = (platformType == null && other.platformType == null) ||
                    (platformType != null && other.platformType != null && platformType.equals(other.platformType));

            boolean sameAuthorisationKey = (authorisationKey == null && other.authorisationKey == null) ||
                    (authorisationKey != null && other.authorisationKey != null && authorisationKey.equals(other.authorisationKey));

            boolean sameCertificateBody = (certificateBody == null && other.certificateBody == null) ||
                    (certificateBody != null && other.certificateBody != null && certificateBody.equals(other.certificateBody));

            boolean samePackageUri = (packageUri == null && other.packageUri == null) ||
                    (packageUri != null && other.packageUri != null && packageUri.equals(other.packageUri));

            // Match everything.
            return (samePlatform && sameAuthorisationKey && sameCertificateBody && samePackageUri);
        }
        return obj.equals(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
