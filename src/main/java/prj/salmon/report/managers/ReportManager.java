package prj.salmon.report.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import prj.salmon.report.utils.WebhookSender;

import java.util.*;

public class ReportManager {
    private final JavaPlugin plugin;
    private WebhookSender webhookSender;
    private final Map<UUID, SessionState> sessions = new HashMap<>();
    private final Set<String> blockedReports = new HashSet<>();
    private final Map<UUID, Deque<Long>> reportHistory = new HashMap<>();

    public ReportManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        String webhookUrl = config.getString("webhook-url", "");
        
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL が設定されていません！");
        } else {
            webhookSender = new WebhookSender(webhookUrl);
        }

        blockedReports.clear();
        blockedReports.addAll(config.getStringList("blockedReports"));
    }

    public boolean isBlocked(String playerName) {
        return blockedReports.contains(playerName);
    }

    public void setBlocked(String playerName, boolean blocked) {
        if (blocked) {
            blockedReports.add(playerName);
        } else {
            blockedReports.remove(playerName);
        }
        plugin.getConfig().set("blockedReports", new ArrayList<>(blockedReports));
        plugin.saveConfig();
    }

    public boolean canReport(UUID playerUuid) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = reportHistory.computeIfAbsent(playerUuid, k -> new ArrayDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 300_000) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= 10) return false;

        timestamps.addLast(now);
        return true;
    }

    public SessionState getSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    public SessionState startSession(UUID playerUuid) {
        SessionState session = new SessionState();
        sessions.put(playerUuid, session);
        return session;
    }

    public void endSession(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public WebhookSender getWebhookSender() {
        return webhookSender;
    }

    public static class SessionState {
        public String reason;
        public String reasonDetail;
        public String harassmentDetail;
        public String target;
        public boolean waitingReasonText;
        public boolean waitingPlayerName;
        public boolean waitingHarassmentDetail;
        public boolean isTransitioning;
    }
}
