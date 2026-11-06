package prj.salmon.report.utils;

/** Makes player text safe to show in Discord: nothing in it acts as formatting, and it fits the given limit. */
final class DiscordText {
    // Everything Discord treats as formatting, a link, a mention, an emoji or a timestamp.
    private static final String SPECIAL = "\\*_~`|>#-+[]()<@:";
    private static final String ELLIPSIS = "…";

    private DiscordText() {
    }

    static String sanitize(String text, int maxLength) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        text.codePoints().forEach(codePoint -> {
            int type = Character.getType(codePoint);
            if (Character.isISOControl(codePoint) || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                // One line only, so that the text cannot look like a separate part of the report.
                out.append(' ');
            } else if (type != Character.FORMAT) {
                // Format characters are invisible and can reorder what is displayed.
                if (SPECIAL.indexOf(codePoint) >= 0) {
                    out.append('\\');
                }
                out.appendCodePoint(codePoint);
            }
        });
        return truncate(out, maxLength);
    }

    /** Lengths are in UTF-16 units, which is never less than the code points Discord may count. */
    private static String truncate(CharSequence text, int maxLength) {
        if (text.length() <= maxLength) {
            return text.toString();
        }
        int end = maxLength - ELLIPSIS.length();
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        // A backslash left at the end would escape the ellipsis.
        int backslashes = 0;
        while (backslashes < end && text.charAt(end - 1 - backslashes) == '\\') {
            backslashes++;
        }
        if (backslashes % 2 == 1) {
            end--;
        }
        return text.subSequence(0, end) + ELLIPSIS;
    }
}
