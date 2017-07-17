import dao.AccountDao;
import enums.pushservices.PlatformType;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AccountTest extends CommuteTestApplication {
    private static AccountDao mAccountDao;

    @BeforeClass
    public static void initialise() {
        mAccountDao = application.injector().instanceOf(AccountDao.class);
    }

    @Before
    public void beforeTest() {
    }

    @After
    public void afterTest() {
    }

    @Test
    public void testDatabaseAccountInsert() {
        PlatformAccount platformAccount = new PlatformAccount();
        platformAccount.authorisationKey = "test_auth_key";
        platformAccount.platformType = PlatformType.SERVICE_GCM;

        // Save the account
        Account newAccount = new Account();
        newAccount.apiKey = "test_api_key";
        newAccount.orgName = "Test Organisation";
        newAccount.active = false;
        newAccount.email = "test@example.com";
        newAccount.dailyEstLimit = 51200L;
        newAccount.platformAccounts = Arrays.asList(platformAccount);
        assertTrue(mAccountDao.saveAccount(newAccount));

        // Get the account
        Account savedAccount = mAccountDao.getAccountForKey("test_api_key");
        assertNotNull(savedAccount);
        assertNotNull(savedAccount.platformAccounts);
        assertFalse(savedAccount.platformAccounts.isEmpty());

        // Remove it.
        assertTrue(mAccountDao.removeAccount(savedAccount.id));
    }

    @Test
    public void testDatabaseAccountUpdate() {
        List<PlatformAccount> platformAccounts = new ArrayList<>();
        PlatformAccount platformAccount = new PlatformAccount();
        platformAccount.authorisationKey = "test_auth_key";
        platformAccount.platformType = PlatformType.SERVICE_GCM;
        platformAccounts.add(platformAccount);

        // Save the account
        Account newAccount = new Account();
        newAccount.apiKey = "test_api_key";
        newAccount.orgName = "Test Organisation";
        newAccount.active = false;
        newAccount.email = "test@example.com";
        newAccount.dailyEstLimit = 51200L;
        newAccount.platformAccounts = platformAccounts;
        mAccountDao.saveAccount(newAccount);

        // Update the account.
        Account savedAccount = mAccountDao.getAccountForKey("test_api_key");
        assertNotNull(savedAccount);
        assertNotNull(savedAccount.platformAccounts);

        PlatformAccount newPlatformAccount = new PlatformAccount();
        newPlatformAccount.authorisationKey = "test_auth_key2";
        newPlatformAccount.platformType = PlatformType.SERVICE_APNS;
        savedAccount.platformAccounts.add(newPlatformAccount);
        savedAccount.email = "change@example.com";

        assertTrue(mAccountDao.saveAccount(savedAccount));

        // Get the account
        Account updatedAccount = mAccountDao.getAccountForKey("test_api_key");
        assertNotNull(updatedAccount);
        assertNotNull(updatedAccount.platformAccounts);
        assertEquals(updatedAccount.email, "change@example.com");
        assertFalse(updatedAccount.platformAccounts.isEmpty());
        assertEquals(updatedAccount.platformAccounts.size(), 2);

        // Remove it.
        assertTrue(mAccountDao.removeAccount(updatedAccount.id));
    }
}
