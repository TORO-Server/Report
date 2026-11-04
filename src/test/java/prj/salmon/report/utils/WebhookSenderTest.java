package prj.salmon.report.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSenderTest {
    private static JsonObject payload(String reporter, String target, String reason, String detail) {
        return JsonParser.parseString(WebhookSender.buildPayload(reporter, target, reason, detail, "(X: 1.0 Y: -2.0 Z: 3.0)")).getAsJsonObject();
    }

    private static JsonArray fields(JsonObject payload) {
        return payload.getAsJsonArray("embeds").get(0).getAsJsonObject().getAsJsonArray("fields");
    }

    private static String value(JsonArray fields, int index) {
        return fields.get(index).getAsJsonObject().get("value").getAsString();
    }

    @Test
    void mentionsNeverNotifyAnyone() {
        JsonObject allowedMentions = payload("Steve", "Alex", "その他", null).getAsJsonObject("allowed_mentions");
        assertEquals(0, allowedMentions.getAsJsonArray("parse").size());
    }

    @Test
    void playerTextIsEscaped() {
        JsonArray fields = fields(payload("a_b", "@everyone", "[x](https://example.com)", "**d**"));
        assertEquals("a\\_b", value(fields, 0));
        assertEquals("\\@everyone", value(fields, 1));
        assertEquals("\\[x\\]\\(https\\://example.com\\)", value(fields, 3));
        assertEquals("\\*\\*d\\*\\*", value(fields, 4));
    }

    @Test
    void quotesAndLineBreaksStayInsideTheField() {
        JsonArray fields = fields(payload("Steve", "Alex", "その他", "a\"b\nc\\d"));
        assertEquals("a\"b c\\\\d", value(fields, 4));
    }

    @Test
    void everyFieldFitsDiscordLimits() {
        String longText = "*".repeat(5000);
        JsonObject payload = payload(longText, longText, longText, longText);
        JsonArray fields = fields(payload);
        int total = payload.getAsJsonArray("embeds").get(0).getAsJsonObject().get("title").getAsString().length();
        assertEquals(5, fields.size());
        for (int i = 0; i < fields.size(); i++) {
            JsonObject field = fields.get(i).getAsJsonObject();
            String name = field.get("name").getAsString();
            String value = field.get("value").getAsString();
            assertTrue(name.length() <= 256);
            assertTrue(value.length() <= 1024, "field " + i + " is " + value.length());
            total += name.length() + value.length();
        }
        assertTrue(total <= 6000, "embed is " + total);
    }

    @Test
    void emptyValuesAreReplacedBecauseDiscordRejectsThem() {
        JsonArray fields = fields(payload("Steve", null, "   ", null));
        assertEquals(4, fields.size());
        assertFalse(value(fields, 1).isBlank());
        assertFalse(value(fields, 3).isBlank());
    }
}
