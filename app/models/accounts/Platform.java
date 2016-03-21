package models.accounts;

import com.avaje.ebean.Model;
import main.Constants;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "platform", schema = "service_accounts")
public class Platform extends Model {
    public static Finder<String, Platform> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Platform.class);

    @Transient
    public final static String PLATFORM_NAME_GCM = "gcm";
    @Transient
    public final static String PLATFORM_NAME_APNS = "apns";

    public Platform() {
    }

    @Id
    @Column(name = "platform_name")
    public String platformName;

    @Column(name = "endpoint_url")
    public String endpointUrl;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "platform", fetch = FetchType.LAZY)
    public List<PlatformAccount> platformAccounts;
}
