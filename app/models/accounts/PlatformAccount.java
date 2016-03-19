package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;
import models.alerts.Agency;
import models.registrations.Registration;

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

    @Column(name = "client_key")
    public String clientKey;

    @ManyToOne(fetch = FetchType.LAZY)
    public Platform platform;
}
