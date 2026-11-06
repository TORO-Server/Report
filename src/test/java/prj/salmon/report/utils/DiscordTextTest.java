package prj.salmon.report.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscordTextTest {
    private static final int LIMIT = 1024;

    @Test
    void formattingIsEscaped() {
        assertEquals("\\*\\*bold\\*\\* \\_a\\_ \\~\\~b\\~\\~ \\`c\\` \\|\\|d\\|\\|", DiscordText.sanitize("**bold** _a_ ~~b~~ `c` ||d||", LIMIT));
        assertEquals("\\> quote \\# title \\- item \\+ item", DiscordText.sanitize("> quote # title - item + item", LIMIT));
    }

    @Test
    void linksMentionsAndEmojiDoNotWork() {
        assertEquals("\\[text\\]\\(https\\://example.com\\)", DiscordText.sanitize("[text](https://example.com)", LIMIT));
        assertEquals("\\@everyone \\<\\@123\\> \\<\\#456\\> \\<\\:name\\:789\\>", DiscordText.sanitize("@everyone <@123> <#456> <:name:789>", LIMIT));
    }

    @Test
    void aBackslashCannotUndoTheEscaping() {
        assertEquals("\\\\\\*", DiscordText.sanitize("\\*", LIMIT));
    }

    @Test
    void ordinaryTextIsUnchanged() {
        assertEquals("Steve を通報します 123 ABC", DiscordText.sanitize("Steve を通報します 123 ABC", LIMIT));
    }

    @Test
    void textStaysOnOneLineAndInvisibleCharactersAreRemoved() {
        assertEquals("a b  c d e", DiscordText.sanitize("a\nb\r\nc d e", LIMIT));
        assertEquals("abc", DiscordText.sanitize("a‮b​c", LIMIT));
    }

    @Test
    void textWithinTheLimitIsKept() {
        String text = "a".repeat(LIMIT);
        assertEquals(text, DiscordText.sanitize(text, LIMIT));
    }

    @Test
    void longTextIsCutToTheLimit() {
        String result = DiscordText.sanitize("a".repeat(LIMIT + 1), LIMIT);
        assertEquals(LIMIT, result.length());
        assertTrue(result.endsWith("…"));
    }

    @Test
    void escapingIsCountedInTheLimit() {
        String result = DiscordText.sanitize("*".repeat(LIMIT), LIMIT);
        assertTrue(result.length() <= LIMIT);
        assertTrue(result.endsWith("\\*…"), result.substring(result.length() - 5));
    }

    @Test
    void cuttingNeverLeavesAnEscapeForTheEllipsis() {
        // The cut falls right after the backslash of the last character.
        String result = DiscordText.sanitize("a" + "*".repeat(LIMIT), LIMIT);
        assertTrue(result.length() <= LIMIT);
        assertTrue(result.endsWith("\\*…"), result.substring(result.length() - 5));
    }

    @Test
    void cuttingNeverSplitsASurrogatePair() {
        String result = DiscordText.sanitize("a" + "😀".repeat(LIMIT), LIMIT);
        assertTrue(result.length() <= LIMIT);
        assertFalse(Character.isHighSurrogate(result.charAt(result.length() - 2)));
        assertTrue(result.endsWith("😀…") || result.endsWith("a…"));
    }
}
