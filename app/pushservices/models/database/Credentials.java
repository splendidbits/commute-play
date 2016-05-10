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

    @SuppressWarnings("unused")
    public Credentials() {

    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += platformType != null
                ? platformType.hashCode()
                : hashCode;

        hashCode += authorisationKey != null
                ? authorisationKey.hashCode()
                : hashCode;

        hashCode += certificateBody != null
                ? certificateBody.hashCode()
                : hashCode;

        hashCode += packageUri != null
                ? packageUri.hashCode()
                : hashCode;

        return hashCode.hashCode();
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
}
