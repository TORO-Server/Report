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
            connection.setDoOutput(true);

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

            String jsonPayload = root.toString();

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

    private JsonObject createField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value != null ? value : "");
        if (inline) {
            field.addProperty("inline", true);
        }
        return field;
    }
}
