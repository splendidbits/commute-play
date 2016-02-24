package main;

import models.alerts.Alert;

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
    public static Alert escapedAlert(Alert alert) {
        if (alert != null) {
            if (alert.advisoryMessage != null) {
                alert.advisoryMessage = escapeSpecials(alert.advisoryMessage);
            }
            if (alert.currentMessage != null) {
                alert.currentMessage = escapeSpecials(alert.currentMessage);
            }
            if (alert.detourMessage != null) {
                alert.detourMessage = escapeSpecials(alert.detourMessage);
            }
            if (alert.detourReason != null) {
                alert.detourReason = escapeSpecials(alert.detourReason);
            }

            return alert;
        }
        return null;
    }

    public static String escapeSpecials(String string) {
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

    public static String escapeNonHtml(String string) {
        String newSearch = string.replaceAll("(?=[]\\[+&|!(){}^\"~*?:\\\\-])", "\\\\");
        return newSearch;
    }
}
