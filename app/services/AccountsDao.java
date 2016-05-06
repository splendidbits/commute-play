package services;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
import pushservices.enums.PlatformType;
import models.accounts.Account;
import models.accounts.Platform;
import models.alerts.Route;
import services.splendidlog.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * A DAO class for platform + API account functions.
 * <p>
 */
public class AccountsDao {
    private static final String TAG = AccountsDao.class.getSimpleName();

    private EbeanServer mEbeanServer;
    private Log mLog;

    @Inject
    public AccountsDao(EbeanServer ebeanServer, Log log) {
        mEbeanServer = ebeanServer;
        mLog = log;
    }

    /**
     * Fetch list of API accounts for a given route and push service type.
     *
     * @param platform Push service platform type.
     * @param agencyId Id of {@link models.alerts.Agency}
     * @param route An Agency {@link Route}.
     *
     * @return List of API accounts.
     */
    @Nullable
    public List<Account> getAccounts(@Nonnull PlatformType platform, int agencyId, @Nonnull Route route) {
        try {
            List<Account> accounts = mEbeanServer.find(Account.class)
                    .fetch("devices", new FetchConfig().query())
                    .fetch("devices.subscriptions", new FetchConfig().query())
                    .fetch("platformAccounts", new FetchConfig().query())
                    .fetch("platformAccounts.platform", new FetchConfig().query())
                    .where()
                    .conjunction()
                    .eq("platformAccounts.platform.platformType", platform)
                    .eq("devices.subscriptions.route.routeId", route.routeId)
                    .eq("devices.subscriptions.route.agency.id", agencyId)
                    .endJunction()
                    .filterMany("platformAccounts").eq("platform.platformType", platform)
                    .filterMany("devices").eq("subscriptions.route.agency.id", agencyId)
                    .filterMany("devices").eq("subscriptions.route.routeId", route.routeId)
                    .findList();

            String logString = accounts != null && !accounts.isEmpty()
                    ? String.format("Found %d accounts for route %s", accounts.size(), route.routeId)
                    : String.format("No accounts found for route %s", route.routeId);

            mLog.d(TAG, logString);
            return accounts;

        } catch (Exception e) {
            mLog.e(TAG, "Error persisting subscription", e);
        }
        return null;
    }

    /**
     * Get a API service Account.
     *
     * @param email registered email address for account.
     * @return an Account object, null if not found.
     */
    @Nullable
    public Account getAccountForEmail(@Nonnull String email) {
        Account account = mEbeanServer.createQuery(Account.class)
                .fetch("platformAccounts")
                .fetch("platformAccounts.platform")
                .where()
                .eq("email", email)
                .findUnique();

        String logString = account != null
                ? String.format("Found an API account for email %s", email)
                : String.format("No API account found for email %s", email);

        mLog.d(TAG, logString);
        return account;
    }

    /**
     * Get a API service Account.
     *
     * @param apiKey api key for which account was assigned.
     * @return an Account object, null if not found.
     */
    @Nullable
    public Account getAccountForKey(@Nonnull String apiKey) {
        Account account = mEbeanServer.createQuery(Account.class)
                .where()
                .eq("apiKey", apiKey)
                .findUnique();

        String logString = account != null
                ? String.format("Found an API account found for key %s", apiKey)
                : String.format("No API account found for key %s", apiKey);

        mLog.d(TAG, logString);
        return account;
    }

    /**
     * Get a platform model for name, such as GCM or APNS.
     *
     * @param platformType type of platform.
     * @return Platform. null if not found
     */
    @Nullable
    public Platform getPlatform(@Nonnull PlatformType platformType) {
        return Platform.find.byId(platformType.name);
    }
}
