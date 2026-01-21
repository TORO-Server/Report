package prj.salmon.report;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.bukkit.Bukkit.getLogger;

public class webhooksender {
    private final String webhookUrl;

    public webhooksender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendReport(String reporter, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URLが設定されていません。送信スキップします。");

            return;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(webhookUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            content = content.replace("\"", "\\\"").replace("\n", "\\n");

            String jsonPayload = """
                {
                    "username": "通報",
                    "content": "**通報者:** %s\\n%s"
                }
                """.formatted(
                    reporter.replace("\"", "\\\""),
                    content.replace("\"", "\\\"")
            );


            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
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
}
