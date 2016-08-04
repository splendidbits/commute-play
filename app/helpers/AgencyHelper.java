package helpers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import javax.annotation.Nonnull;

/**
 * Utils to assist with cleaning up various things
 * with alerts.
 */
public class AgencyHelper {

    /**
     * Strip a string of HTML but preserve all kinds of line-breaks.
     *
     * @param htmlString string to parse
     * @return formatted string stripped of HTML.
     */
    public static String formatStringPlainText(@Nonnull String htmlString) {
        return Jsoup.clean(htmlString, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }
}
