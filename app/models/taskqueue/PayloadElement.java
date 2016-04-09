package models.taskqueue;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import main.Constants;

import javax.persistence.*;

@Entity
@Table(name = "payload_element", schema = "task_queue")
public class PayloadElement extends Model {
    public static Finder<Integer, PayloadElement> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, PayloadElement.class);

    @Id
    @Column(name = "element_id")
    @SequenceGenerator(name = "element_id_seq_gen", sequenceName = "element_id_seq", allocationSize = 1)
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_id_seq_gen")
    public Integer elementId;

    @Column(name = "element_name")
    public String name;

    @Column(name = "element_value")
    public String value;

    @ManyToOne(fetch = FetchType.EAGER)
    public Message message;

    public PayloadElement() {

    }

    public PayloadElement(String name, String value) {
        this.name = name;
        this.value = value;
    }
}