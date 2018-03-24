package dao;

import enums.pushservices.PlatformType;
import io.ebean.EbeanServer;
import io.ebean.FetchConfig;
import io.ebean.Junction;
import models.accounts.Account;
import models.alerts.Route;
import play.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * A DAO class for platform + API account functions.
 * <p>
 */
public class AccountDao extends BaseDao {

    @Inject
    public AccountDao(EbeanServer ebeanServer) {
        super(ebeanServer);
    }

    /**
     * Fetch list of API accounts for a given route and push service type,
     * containing a filtered list of devices that have subscriptions for the
     * given routeId.
     *
     * @param platform Push service platform type.
     * @param agencyId Id of {@link models.alerts.Agency}
     * @param routeId  An Agency {@link Route} routeId.
     * @return List of API accounts.
     */
    @Nonnull
    public List<Account> getAccounts(@Nonnull PlatformType platform, int agencyId, @Nonnull String routeId) {
        List<Account> accounts = new ArrayList<>();

        try {
            accounts = mEbeanServer.find(Account.class)
                    .fetch("devices", new FetchConfig().query())
                    .fetch("devices.subscriptions", new FetchConfig().query())
                    .fetch("platformAccounts", new FetchConfig().query())
                    .where()
                    .conjunction()
                    .eq("platformAccounts.platformType", platform)
                    .eq("devices.subscriptions.route.routeId", routeId)
                    .eq("devices.subscriptions.route.agency.id", agencyId)
                    .endJunction()
                    .filterMany("platformAccounts").eq("platformType", platform)
                    .filterMany("devices").eq("subscriptions.route.agency.id", agencyId)
                    .filterMany("devices").eq("subscriptions.route.routeId", routeId)
                    .findList();

            String logString = accounts != null && !accounts.isEmpty()
                    ? String.format("Found %d accounts for route %s", accounts.size(), routeId)
                    : String.format("No accounts found for route %s", routeId);

            Logger.debug(logString);

        } catch (Exception e) {
            Logger.error("Error fetching account.", e);
        }

        return accounts != null ? accounts : new ArrayList<>();
    }

    /**
     * Get a API service Account.
     *
     * @param apiKey api key for which account was assigned.
     * @return an Account object, null if not found.
     */
    @Nullable
    public Account getAccountForKey(String apiKey) {
        if (apiKey != null) {
            Account account = mEbeanServer.createQuery(Account.class)
                    .where()
                    .eq("apiKey", apiKey)
                    .findOne();

            String logString = account != null
                    ? String.format("Found an API account found for key %s", apiKey)
                    : String.format("No API account found for key %s", apiKey);

            Logger.debug(logString);
            return account;
        }
        return null;
    }

    /**
     * Save an account.
     *
     * @param account to save.
     * @return boolean of success.
     */
    public boolean saveAccount(Account account) {
        try {
            Junction<Account> accountSearch = mEbeanServer.find(Account.class)
                    .fetch("platformAccounts")
                    .where()
                    .disjunction();

            if (account.id != null) {
                accountSearch.idEq(account.id);
            } else if (account.apiKey != null) {
                accountSearch.eq("api_key", account.apiKey);
            }
            accountSearch.endJunction();

            Account savedAccount = accountSearch.endJunction().findOne();
            if (savedAccount != null) {
                account.id = savedAccount.id;
                mEbeanServer.save(account);

            } else {
                mEbeanServer.insert(account);
            }

            return true;

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
            return false;
        }
    }

    /**
     * Remove an account.
     *
     * @param accountId accountId
     * @return boolean of success.
     */
    public boolean removeAccount(long accountId) {
        try {
            Account account = mEbeanServer.find(Account.class)
                    .fetch("platformAccounts")
                    .where()
                    .idEq(accountId)
                    .findOne();

            mEbeanServer.deletePermanent(account);
            return true;

        } catch (Exception e) {
            Logger.error("Error deleting agency.", e);
            return false;
        }
    }
}
