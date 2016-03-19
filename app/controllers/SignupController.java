package controllers;

import com.avaje.ebean.Ebean;
import helpers.ValidationHelper;
import models.accounts.Account;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.signup;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Adds an API user to the system to have access to push services.
 */
public class SignupController extends Controller {
    private static final String DEVICE_UUID_KEY = "devuuid";
    private static final String REGISTRATION_TOKEN_KEY = "devregid";

    // Return results
    private static final Result MISSING_PARAMS_RESULT = badRequest("Invalid registration parameters in request.");
    private static final Result BAD_CLIENT_RESULT = badRequest("Calling client is invalid.");

    public Result signup() {
        return ok(signup.render(null, DynamicForm.form()));
    }

    /**
     * Register an API user with the commute GCM server. By default sets the user
     * as not activated.
     *
     * @return a Result.
     */
    @Transactional
    public Result addUser() {
        DynamicForm signupForm = Form.form().bindFromRequest();
        if (signupForm.hasErrors()) {
            return badRequest(signup.render("There were errors with your application", signupForm));
        }

        Map<String, String> formData = validateSignupData(signupForm).data();
        String organisationName = formData.get("org_name");
        String email = formData.get("email");
        String password1 = formData.get("password_1");
        String password2 = formData.get("password_2");
        String estDailyRegistrations = formData.get("estimate_daily_regs");

        Account pendingAccount = new Account();
        pendingAccount.orgName = organisationName;
        pendingAccount.email = email;
        pendingAccount.passwordHash = DigestUtils.sha1Hex(password1);
        pendingAccount.apiKey = RandomStringUtils.random(7, true, false);
        pendingAccount.estSendLimit = Long.valueOf(estDailyRegistrations);
        pendingAccount.monthlySendLimit = 1000000L;
        pendingAccount.active = false;
        Ebean.save(pendingAccount);

        return ok(signup.render("Your API request has been submitted. Mark email from @splendidbits.co as non-spam",
                new DynamicForm()));
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