package models.accounts;

import com.avaje.ebean.Model;
import enums.PlatformType;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "platform", schema = "service_accounts")
public class Platform extends Model {
    public static Finder<String, Platform> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Platform.class);

    public Platform() {
    }

    @Id
    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    public PlatformType platform;

    @Column(name = "endpoint_url")
    public String endpointUrl;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "platform", fetch = FetchType.LAZY)
    public List<PlatformAccount> platformAccounts;
}
