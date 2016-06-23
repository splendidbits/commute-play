package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.FetchConfig;
import enums.pushservices.PlatformType;
import models.accounts.Account;
import models.alerts.Route;
import services.splendidlog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

/**
 * A DAO class for platform + API account functions.
 * <p>
 */
public class AccountsDao extends BaseDao {

    @Inject
    public AccountsDao(EbeanServer ebeanServer) {
        super(ebeanServer);
    }

    /**
     * Fetch list of API accounts for a given route and push service type,
     * containing a filtered list of devices that have subscriptions for the
     * given routeId.
     *
     * @param platform Push service platform type.
     * @param agencyId Id of {@link models.alerts.Agency}
     * @param route An Agency {@link Route}.
     *
     * @return List of API accounts.
     */
    @Nullable
    public List<Account> getAccountDevices(@Nonnull PlatformType platform, int agencyId, @Nonnull Route route) {
        try {
            List<Account> accounts = mEbeanServer.find(Account.class)
                    .fetch("devices", new FetchConfig().query())
                    .fetch("devices.subscriptions", new FetchConfig().query())
                    .fetch("platformAccounts", new FetchConfig().query())
                    .where()
                    .conjunction()
                    .eq("platformAccounts.platformType", platform)
                    .eq("devices.subscriptions.route.routeId", route.routeId)
                    .eq("devices.subscriptions.route.agency.id", agencyId)
                    .endJunction()
                    .filterMany("platformAccounts").eq("platformType", platform)
                    .filterMany("devices").eq("subscriptions.route.agency.id", agencyId)
                    .filterMany("devices").eq("subscriptions.route.routeId", route.routeId)
                    .findList();

            String logString = accounts != null && !accounts.isEmpty()
                    ? String.format("Found %d accounts for route %s", accounts.size(), route.routeId)
                    : String.format("No accounts found for route %s", route.routeId);
            Logger.debug(logString);

            return accounts;

        } catch (Exception e) {
            Logger.error("Error fetching device / subscription accounts.", e);
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

        Logger.debug(logString);
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

        Logger.debug(logString);
        return account;
    }
}
