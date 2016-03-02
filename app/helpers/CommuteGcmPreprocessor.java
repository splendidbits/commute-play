package helpers;

import main.Constants;
import models.alerts.Alert;
import models.alerts.Route;
import models.app.GoogleMessage;
import models.app.GoogleResponse;
import models.app.ModifiedAlerts;
import models.registrations.Registration;
import services.GoogleGcmDispatcher;
import services.SubscriptionsService;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * Sets up things for the GCM Dispatcher.
 */
public class CommuteGcmPreprocessor implements GoogleGcmDispatcher.GoogleResponseInterface {
    private static final String GCM_KEY_ROUTE_ID = "route_id";
    private static final String GCM_KEY_ALERT_TYPE = "alert_type";
    private static final String GCM_KEY_ROUTE_NAME = "route_name";
    private static final String GCM_KEY_MESSAGE = "message";

    private enum AlertUpdateType {
        ALERT_CURRENT_MESSAGE("current_alert"),
        ALERT_DETOUR_MESSAGE("detour_alert"),
        ALERT_ADVISORY_MESSAGE("advisory_alert"),
        ALERT_APP_MESSAGE("app_alert"),
        ALERT_CANCEL("cancel_alert");

        private String value;

        AlertUpdateType(String value) {
            this.value = value;
        }
    }

    /**
     * TODO: Migrate this to a polling TaskQueue service.
     *
     * Notify GCM susbcribers of the modified alerts which have changed.
     *
     * @param modifiedAlerts Collection of modified alerts including their routes.
     */
    public void notifyAlertSubscribers(@Nonnull ModifiedAlerts modifiedAlerts) {
        Map<Route, List<Alert>> updatedRoutes = modifiedAlerts.getUpdatedAlerts();

        // Iterate through the routes.
        for (Route route : updatedRoutes.keySet()) {

            SubscriptionsService subscriptionsService = new SubscriptionsService();
            List<Registration> registrations = subscriptionsService.getSubscribedRegistrations(route);

            if (registrations != null && !registrations.isEmpty()) {

                // Loop through each updated alert in the set.
                for (Alert updatedAlert : updatedRoutes.get(route)) {
                    GoogleMessage googleMessage = getGoogleMessage(updatedAlert, registrations);
                    GoogleGcmDispatcher gcmDispatcher = new GoogleGcmDispatcher(Constants.GOOGLE_GCM_AUTH_KEY);
                    gcmDispatcher.sendGcmMessage(googleMessage, this);
                }
            }
        }
    }

    /**
     * Add an alert message (detour, advisory, etc) and a set of
     * registrations to the preprocessor.
     *
     * @param updatedAlert  alert which has been validated as update ready..
     * @param registrations list of registrations.
     */
    private GoogleMessage getGoogleMessage(@Nonnull Alert updatedAlert, List<Registration> registrations) {
        GoogleMessage googleMessage = new GoogleMessage(registrations);
        googleMessage.addData(GCM_KEY_ROUTE_ID, updatedAlert.route.routeId);
        googleMessage.addData(GCM_KEY_ROUTE_NAME, updatedAlert.route.routeName);

        boolean messageDataEmpty = CompareUtils.isEmptyNullSafe(
                updatedAlert.currentMessage,
                updatedAlert.advisoryMessage,
                updatedAlert.detourMessage,
                updatedAlert.detourStartLocation,
                updatedAlert.detourReason);

        boolean datesEmpty = updatedAlert.detourStartDate == null
                && updatedAlert.detourEndDate == null;

        boolean noSnow = !updatedAlert.isSnow;

        if (messageDataEmpty && datesEmpty && noSnow) {
            // If all of the above in the alert was empty, send a cancel alert message.
            googleMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_CANCEL.value);

        } else if (!datesEmpty || !CompareUtils.isEmptyNullSafe(
                updatedAlert.detourMessage,
                updatedAlert.detourStartLocation,
                updatedAlert.detourReason)) {
            // Detour alert if there is some detour information.
            googleMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_DETOUR_MESSAGE.value);
            googleMessage.addData(GCM_KEY_MESSAGE, updatedAlert.detourMessage);
            googleMessage.setCollapseKey(updatedAlert.route.routeId);

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.currentMessage)) {
            // Current Message has updated.
            googleMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_CURRENT_MESSAGE.value);
            googleMessage.addData(GCM_KEY_MESSAGE, updatedAlert.currentMessage);
            googleMessage.setCollapseKey(updatedAlert.route.routeId);

        } else if (!messageDataEmpty && !CompareUtils.isEmptyNullSafe(updatedAlert.advisoryMessage)) {
            // Advisory Message has updated.
            googleMessage.addData(GCM_KEY_ALERT_TYPE, AlertUpdateType.ALERT_ADVISORY_MESSAGE.value);
            googleMessage.addData(GCM_KEY_MESSAGE, updatedAlert.advisoryMessage);
            googleMessage.setCollapseKey(updatedAlert.route.routeId);
        }
        return googleMessage;
    }


    @Override
    public void messageRequestSuccess(GoogleResponse googleResponse) {

    }

    @Override
    public void messageRequestFailed(GoogleGcmDispatcher.CommuteDispatchError commuteDispatchError) {

    }
}
