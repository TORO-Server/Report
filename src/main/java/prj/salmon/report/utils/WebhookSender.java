package prj.salmon.report.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class WebhookSender {
    // Discord's limit for an embed field value. With at most five fields the embed also stays under its total of 6000.
    private static final int FIELD_VALUE_LIMIT = 1024;
    // Discord rejects the whole message if a field value is empty.
    private static final String EMPTY_FIELD_VALUE = "(なし)";
    private static final int TIMEOUT_MILLIS = 10_000;

    private final String webhookUrl;

    public WebhookSender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public CompletableFuture<Void> sendReportAsync(String reporter, String target, String reason, String detail, String coords) {
        return CompletableFuture.runAsync(() -> sendReport(reporter, target, reason, detail, coords));
    }

    private void sendReport(String reporter, String target, String reason, String detail, String coords) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URLが設定されていません。送信スキップします。");
            return;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(webhookUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setDoOutput(true);

            String jsonPayload = buildPayload(reporter, target, reason, detail, coords);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204 && responseCode != 200) {
                getLogger().warning("Webhook送信失敗: HTTP " + responseCode);

                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) {
                        String errorBody = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                        getLogger().warning("Discord Error Response: " + errorBody);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Webhook送信中に例外が発生しました", e);
        }
    }

    static String buildPayload(String reporter, String target, String reason, String detail, String coords) {
        // Construct JSON using Gson
        JsonObject root = new JsonObject();
        root.addProperty("username", "通報");

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "新規通報");
        embed.addProperty("color", 16711680); // Red

        JsonArray fields = new JsonArray();
        fields.add(createField("通報者", reporter, true));
        fields.add(createField("対象", target, true));
        fields.add(createField("座標", coords, true));
        fields.add(createField("理由", reason, false));

        if (detail != null && !detail.isEmpty()) {
            fields.add(createField("詳細", detail, false));
        }

        embed.add("fields", fields);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        root.add("embeds", embeds);

        // No mention in a report may notify anyone.
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        root.add("allowed_mentions", allowedMentions);

        return root.toString();
    }

    private static JsonObject createField(String name, String value, boolean inline) {
        String safeValue = DiscordText.sanitize(value != null ? value : "", FIELD_VALUE_LIMIT);

        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", safeValue.isBlank() ? EMPTY_FIELD_VALUE : safeValue);
        if (inline) {
            field.addProperty("inline", true);
        }
        return field;
    }
}
