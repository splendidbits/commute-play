package models.accounts;

import com.avaje.ebean.Model;
import enums.PlatformType;
import main.Constants;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "platform", schema = "api_accounts")
public class Platform extends Model {
    public static Finder<String, Platform> find = new Finder<>(Constants.COMMUTE_GCM_DB_SERVER, Platform.class);

    @Id
    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    public PlatformType platform;

    @Column(name = "endpoint_url")
    public String endpointUrl;

    @Nonnull
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "platform", fetch = FetchType.LAZY)
    public List<PlatformAccount> platformAccounts = new ArrayList<>();

    @SuppressWarnings("unused")
    public Platform() {
    }
}
