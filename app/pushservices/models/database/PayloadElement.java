package pushservices.models.database;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@EntityConcurrencyMode(ConcurrencyMode.NONE)
@Table(name = "payload_element", schema = "push_services")
public class PayloadElement extends Model {
    public static Finder<Long, PayloadElement> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, PayloadElement.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "element_id_seq_gen", sequenceName = "element_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_id_seq_gen")
    public Long id;

    @Column(name = "element_name")
    public String name;

    @Column(name = "element_value", columnDefinition = "TEXT")
    public String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "push_services.messages",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Message message;

    @SuppressWarnings("unused")
    public PayloadElement() {
    }

    public PayloadElement(@Nonnull String name, @Nonnull String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += value != null
                ? value.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PayloadElement) {
            PayloadElement other = (PayloadElement) obj;

            boolean sameName = (name == null && other.name == null) ||
                    (name != null && other.name != null && name.equals(other.name));

            boolean sameValue = (value == null && other.value == null) ||
                    (value != null && other.value != null && value.equals(other.value));

            // Match everything.
            return (sameName && sameValue);
        }
        return obj.equals(this);
    }
}