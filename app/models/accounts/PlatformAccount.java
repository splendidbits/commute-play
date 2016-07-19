package models.accounts;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.PlatformType;

import javax.persistence.*;

@Entity
@Table(name = "platform_accounts", schema = "api_accounts")
public class PlatformAccount extends Model {
    public static Finder<String, PlatformAccount> find = new Finder<>(PlatformAccount.class);

    @Id
    @JsonIgnore
    @SequenceGenerator(name = "platform_account_id_seq_gen", sequenceName = "platform_account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "platform_account_id_seq_gen")
    @Column(name = "id")
    protected Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "account_id",
            table = "api_accounts.accounts",
            referencedColumnName = "id")
    public Account account;

    @Column(name = "package_uri")
    public String packageUri;

    @Column(name = "authorisation_key")
    public String authorisationKey;

    @Column(name = "certificate_body", columnDefinition = "TEXT")
    public String certificateBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    public PlatformType platformType;

    public PlatformAccount() {
    }
}
