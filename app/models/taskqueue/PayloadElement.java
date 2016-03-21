package models.taskqueue;

import com.avaje.ebean.Model;

import javax.persistence.*;

@Entity
public class PayloadElement extends Model {

    public PayloadElement() {
    }

    public PayloadElement(String value, String name) {
        this.value = value;
        this.name = name;
    }

    @Id
    @Column(name = "element_id", updatable = true)
    @SequenceGenerator(name = "element_id_seq_gen", sequenceName = "element_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_id_seq_gen")
    public Integer elementId;

    @Column(name = "element_name")
    public String name;

    @Column(name = "element_value")
    public String value;

    @ManyToOne(fetch = FetchType.EAGER)
    public Message message;
}