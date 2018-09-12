package main;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import enums.pushservices.PlatformType;
import models.accounts.Account;
import models.accounts.PlatformAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class AccountDaoTest extends CommuteTestApplication {

    private static TestModelHelper testModelHelper;

    @BeforeClass
    public static void initialise() {
        testModelHelper = new TestModelHelper(Calendar.getInstance(TimeZone.getTimeZone("EST")));

    }

    @Test
    public void testDatabaseAccountInsert() {
        Account newAccount = testModelHelper.createTestAccount();
        assertTrue(mAccountDao.saveAccount(newAccount));

        // Get the account
        Account savedAccount = mAccountDao.getAccountForKey(newAccount.apiKey);
        assertNotNull(savedAccount);
        assertNotNull(savedAccount.platformAccounts);
        assertFalse(savedAccount.platformAccounts.isEmpty());

        // Remove it.
        assertTrue(mAccountDao.removeAccount(savedAccount.id));
    }

    @Test
    public void testDatabaseAccountUpdate() {
        Account newAccount = testModelHelper.createTestAccount();
        mAccountDao.saveAccount(newAccount);

        // Update the account.
        Account savedAccount = mAccountDao.getAccountForKey(newAccount.apiKey);
        assertNotNull(savedAccount);
        assertNotNull(savedAccount.platformAccounts);

        PlatformAccount newPlatformAccount = new PlatformAccount();
        newPlatformAccount.authorisationKey = "test_auth_key2";
        newPlatformAccount.platformType = PlatformType.SERVICE_APNS;

        savedAccount.platformAccounts.add(newPlatformAccount);
        savedAccount.email = "change@example.com";

        assertTrue(mAccountDao.saveAccount(savedAccount));

        // Get the account
        Account updatedAccount = mAccountDao.getAccountForKey(newAccount.apiKey);
        assertNotNull(updatedAccount);
        assertNotNull(updatedAccount.platformAccounts);
        assertFalse(updatedAccount.platformAccounts.isEmpty());
        assertEquals(updatedAccount.platformAccounts.size(), 2);

        // Remove it.
        assertTrue(mAccountDao.removeAccount(updatedAccount.id));
    }
}
