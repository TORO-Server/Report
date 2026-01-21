package prj.salmon.report.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
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

            String safeReporter = escapeJson(reporter);
            String safeTarget = escapeJson(target);
            String safeReason = escapeJson(reason);
            String safeDetail = detail != null ? escapeJson(detail) : "";
            String safeCoords = escapeJson(coords);

            StringBuilder fieldsBuilder = new StringBuilder();
            fieldsBuilder.append("""
                                {
                                    "name": "通報者",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "対象",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "座標",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "理由",
                                    "value": "%s"
                                }
                    """.formatted(safeReporter, safeTarget, safeCoords, safeReason));
            
            if (!safeDetail.isEmpty()) {
                fieldsBuilder.append("""
                        ,
                        {
                            "name": "詳細",
                            "value": "%s"
                        }
                        """.formatted(safeDetail));
            }

            String jsonPayload = """
                {
                    "username": "通報",
                    "embeds": [
                        {
                            "title": "新規通報",
                            "color": 16711680,
                            "fields": [
                                %s
                            ]
                        }
                    ]
                }
                """.formatted(fieldsBuilder.toString());


            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                getLogger().warning("Webhook送信失敗: HTTP " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
