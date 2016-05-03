package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;

@Entity
@Table(name = "platform_accounts", schema = "api_accounts")
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "account_id",
            table = "api_accounts.accounts",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Account account;

    @Column(name = "package_uri")
    public String packageUri;

    @Column(name = "auth_token")
    public String authToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "platform_id",
            table = "api_accounts.platforms",
            referencedColumnName = "platform",
            unique = false,
            updatable = true)
    public Platform platform;
}
