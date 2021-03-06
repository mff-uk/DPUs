package cz.cuni.mff.xrg.uv.extractor.sukl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author Škoda Petr
 */
public class Utils {

    private Utils() {
    }

    /**
     * Spaces are replaced by '-'.
     *
     * @param part
     * @return
     */
    public static String convertStringToURIPart(String part) {
        // fix spaces
        part = part.replaceAll("\\s+", "-");
        // encode the rest
        try {
            return URLEncoder.encode(part, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unsupported encoding", ex);
        }
    }

    /**
     *
     * @param text
     * @return Index of opening brace to the last brace in given text.
     */
    public static int getLastOpeningBraceIndex(String text) {
        int braceIndex = 1;
        for (int index = text.lastIndexOf(')') - 1; index >= 0; index--) {

            if (text.charAt(index) == '(') {
                braceIndex--;
            } else if (text.charAt(index) == ')') {
                braceIndex++;
            }

            if (braceIndex == 0) {
                return index;
            }
        }
        return -1;
    }

}
