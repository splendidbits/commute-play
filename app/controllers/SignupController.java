package controllers;

import com.avaje.ebean.EbeanServer;
import enums.pushservices.PlatformType;
import helpers.ValidationHelper;
import models.accounts.Account;
import models.accounts.PlatformAccount;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;
import services.fluffylog.Logger;
import views.html.signup;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds an API user to the system to have access to push services.
 */
public class SignupController extends Controller {
    private static final String COMMUTE_IO_API_KEY = "UfhV6Lt";

    @Inject
    private EbeanServer mEbeanServer;

    @Inject
    private FormFactory mFormFactory;

    public Result signup() {
        return ok(signup.render(null, mFormFactory.form()));
    }

    public Result signup(Html html) {
        if (html != null) {
            return ok(html);
        }
        return ok(signup.render(null, mFormFactory.form()));
    }

    /**
     * Register an API user with the commute GCM server. By default sets the user
     * as not activated.
     *
     * @return a Result.
     */
    public Result addUser() {
        DynamicForm signupForm = mFormFactory.form().bindFromRequest();

        Map<String, String> formData = validateSignupData(signupForm).data();
        if (signupForm.hasErrors()) {
            return badRequest(signup.render("There were errors with your application", signupForm));
        }

        String organisationName = formData.get("org_name");
        String email = formData.get("email");
        String password1 = formData.get("password_1");
        String password2 = formData.get("password_2");
        String estDailyRegistrations = formData.get("estimate_daily_regs");
        String packageUri = formData.get("package_uri");
        boolean requiresGcm = formData.get("platform_gcm") != null && formData.get("platform_gcm").equals("on");
        boolean requiresApns = formData.get("platform_apns") != null && formData.get("platform_apns").equals("on");

        PlatformType gcmPlatform = PlatformType.SERVICE_GCM;

        List<PlatformAccount> platformAccounts = new ArrayList<>();
        if (requiresGcm) {
            PlatformAccount gcmAccount = new PlatformAccount();
            gcmAccount.packageUri = packageUri;
            gcmAccount.platformType = PlatformType.SERVICE_GCM;
            platformAccounts.add(gcmAccount);
        }
        if (requiresApns) {
            PlatformAccount apnsAccount = new PlatformAccount();
            apnsAccount.packageUri = packageUri;
            apnsAccount.platformType = PlatformType.SERVICE_GCM;
            platformAccounts.add(apnsAccount);
        }

        if (!platformAccounts.isEmpty()) {
            Account pendingAccount = new Account();
            pendingAccount.orgName = organisationName;
            pendingAccount.email = email;
            pendingAccount.passwordHash = DigestUtils.sha1Hex(password1);
            pendingAccount.dailyEstLimit = Long.valueOf(estDailyRegistrations);
            pendingAccount.dailySendLimit = 1000000L;
            pendingAccount.active = false;
            pendingAccount.platformAccounts = platformAccounts;
            pendingAccount.apiKey = email.equals("daniel@staticfish.com")
                    ? COMMUTE_IO_API_KEY
                    : RandomStringUtils.random(7, true, false);

            mEbeanServer.save(pendingAccount);

        } else {
            Logger.warn("No platforms were found for account signup: " + email);
        }
        Html formResult = signup.render("API request submitted. Check email junk-folder for messages from help@splendidbits.co",
                new DynamicForm(formData, signupForm.errors(), null, null, null, null));

        return signup(formResult);
    }

    /**
     * Validate the signup input data.
     *
     * @param signupForm the form which the user sent
     * @return the dynamicform with any errors added.
     */
    private DynamicForm validateSignupData(@Nonnull DynamicForm signupForm) {
        if (signupForm.data() != null) {
            Map<String, String> formData = signupForm.data();

            String organisationName = formData.get("org_name");
            String email = formData.get("email");
            String password1 = formData.get("password_1");
            String password2 = formData.get("password_2");
            String estDailyRegistrations = formData.get("estimate_daily_regs");
            String packageUri = formData.get("package_uri");

            boolean requiresGcm = formData.get("platform_gcm") != null &&
                    formData.get("platform_gcm").equals("on");

            boolean requiresApns = formData.get("platform_apns") != null
                    && formData.get("platform_apns").equals("on");

            boolean emailGood = email != null &&
                    !email.isEmpty() &&
                    email.contains("@") &&
                    email.contains(".");

            boolean passwordGood = password1 != null &&
                    password2 != null &&
                    password1.equals(password2) &&
                    password1.length() > 5;

            boolean orgNameGood = organisationName != null && organisationName.length() > 3;

            boolean estimatedRegsGood = estDailyRegistrations != null &&
                    ValidationHelper.isNumeric(estDailyRegistrations) &&
                    Integer.valueOf(estDailyRegistrations) > 0 &&
                    Integer.valueOf(estDailyRegistrations) < 1000000;

            boolean platformsGood = requiresApns || requiresGcm;

            if (!passwordGood) {
                signupForm.reject(new ValidationError("password_1", "Passwords must match and be more than 5 characters."));
                Logger.info("Account Signup, Signup password requirements not satisfied: " + email);
            }

            if (!emailGood) {
                signupForm.reject(new ValidationError("email", "Enter a valid email address."));
            }

            if (!orgNameGood) {
                signupForm.reject(new ValidationError("org_name", "Organisation name must have more than 4 characters."));
            }

            if (!estimatedRegsGood) {
                signupForm.reject(new ValidationError("estimate_daily_regs", "Estimation between 0 and 100000"));
            }

            if (!platformsGood) {
                signupForm.reject(new ValidationError("platform_gcm", "One platform must be selected"));
            }
            return signupForm;
        }
        signupForm.reject(new ValidationError("org_name", "Missing Form Fields"));
        return signupForm;
    }

}
