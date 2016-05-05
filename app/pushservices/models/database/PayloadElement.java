package pushservices.models.database;

import com.avaje.ebean.Model;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@Table(name = "payload_element", schema = "task_queue")
public class PayloadElement extends Model {
    public static Finder<Long, PayloadElement> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, PayloadElement.class);

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "element_id_seq_gen", sequenceName = "element_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_id_seq_gen")
    public Long id;

    @Column(name = "element_name")
    public String name;

    @Column(name = "element_value")
    public String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "task_queue.messages",
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
}