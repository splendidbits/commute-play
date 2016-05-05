package helpers;

import models.alerts.Alert;
import models.alerts.Location;

import javax.annotation.Nonnull;

/**
 * Utils to assist with cleaning up various things
 * with alerts.
 */
public class RouteAlertsHelper {

    /**
     * Fix all the strings in an alert converting ascii special chars
     * to html symbols, ready for SQL.
     *
     * @param alert Alert to escape into sql.
     * @return SQL safe escaped alert.
     */
    public static Alert escapeAlert(@Nonnull Alert alert) {

        // Escape the alert title.
        if (alert.messageTitle != null) {
            alert.messageTitle = escapeSpecials(alert.messageTitle);
        }

        // Escape the alert subtitle.
        if (alert.messageSubtitle != null) {
            alert.messageSubtitle = escapeSpecials(alert.messageSubtitle);
        }

        // Escape the alert body.
        if (alert.messageBody != null) {
            alert.messageBody = escapeSpecials(alert.messageBody);
        }

        // Escape the route name.
        if (alert.route != null && alert.route.routeName != null) {
            alert.route.routeName = escapeSpecials(alert.route.routeName);
        }

        if (alert.locations != null) {
            for (Location location : alert.locations) {

                // Escape the location name.
                if (location.name != null) {
                    location.name = escapeSpecials(location.name);
                }

                // Escape the location message.
                if (location.message != null) {
                    location.message = escapeSpecials(location.message);
                }

            }
        }

        return alert;
    }

    /**
     * Remove non html encoding.
     * @param string String to find ascii special characters in.
     *
     * @return Stripped version of special characters.
     */
    private static String escapeSpecials(String string) {
        String escapes[][] = new String[][]{
                {"\\", "\\\\"},
                {"\"", "\\\""},
                {"\n", "\\n"},
                {"\r", "\\r"},
                {"\b", "\\b"},
                {"\f", "\\f"},
                {"\t", "\\t"}
        };
        for (String[] esc : escapes) {
            string = string.replace(esc[0], esc[1]);
        }
        return string;
    }
}
