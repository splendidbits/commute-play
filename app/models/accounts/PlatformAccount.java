package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Agency;
import models.registrations.Registration;
import models.taskqueue.Message;
import models.taskqueue.Recipient;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "platform_account", schema = "service_accounts")
public class PlatformAccount extends Model {
    public static Finder<String, PlatformAccount> find = new Finder<>(
            Constants.COMMUTE_GCM_DB_SERVER, PlatformAccount.class);

    public PlatformAccount() {
    }

    @Id
    @SequenceGenerator(name = "platform_account_id_seq_gen", sequenceName = "platform_account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "platform_account_id_seq_gen")
    @Column(name = "id")
    public Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    public Account account;

    @Column(name = "package_uri")
    public String packageUri;

    @Column(name = "auth_token")
    public String authToken;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    public Platform platform;
}
