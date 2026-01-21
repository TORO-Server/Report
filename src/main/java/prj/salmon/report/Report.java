package prj.salmon.report;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;



public class Report extends JavaPlugin implements Listener {
    private static webhooksender webhookSender;
    private final Map<UUID, SessionState> sessions = new HashMap<>();
    private final Deque<Long> reportTimestamps = new ArrayDeque<>();
    private final Set<String> blockedReports = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String webhookUrl = getConfig().getString("webhook-url", "");
        if (webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URL が設定されていません！");
        } else {
            webhookSender = new webhooksender(webhookUrl);
        }

        blockedReports.clear();
        blockedReports.addAll(getConfig().getStringList("blockedReports"));

        getCommand("report").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) return true;

            if (blockedReports.contains(p.getName())) {
                p.sendMessage("§cあなたは通報を使用できません。");
                return true;
            }

            if (!canReport(p)) {
                p.sendMessage("§c連続しての通報は制限しています。");
                return true;
            }

            openReasonMenu(p);
            return true;
        });
        getCommand("reportblock").setExecutor((sender, cmd, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("§e使用法: /reportblock <プレイヤー名>");
                return true;
            }

            String target = args[0];
            if (blockedReports.contains(target)) {
                blockedReports.remove(target);
                sender.sendMessage("§a" + target + " の通報制限を解除しました。");
            } else {
                blockedReports.add(target);
                sender.sendMessage("§c" + target + " からの通報をブロックしました。");
            }

            getConfig().set("blockedReports", new ArrayList<>(blockedReports));
            saveConfig();
            return true;
        });

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private final Map<UUID, Deque<Long>> reportHistory = new HashMap<>();

    private boolean canReport(Player p) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = reportHistory.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 300_000) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= 10) return false;

        timestamps.addLast(now);
        return true;
    }

    private void openReasonMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "通報理由を選択");

        inv.setItem(11, item(Material.TNT, "破壊(荒らし行為)"));
        inv.setItem(13, item(Material.GRAY_CONCRETE_POWDER, "嫌がらせ"));
        inv.setItem(15, item(Material.YELLOW_CONCRETE, "その他"));

        sessions.put(p.getUniqueId(), new SessionState());
        p.openInventory(inv);
    }

    private void openPlayerMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "誰を通報しますか？");

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            inv.setItem(slot++, targetHead(target));
        }

        inv.setItem(53, item(Material.YELLOW_CONCRETE, "ここにいない"));
        p.openInventory(inv);
    }

    private void openDestructionCheckMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "破壊されているのは現在の場所ですか?");

        inv.setItem(3, item(Material.LIME_CONCRETE, "はい"));
        inv.setItem(5, item(Material.RED_CONCRETE, "いいえ"));

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        SessionState state = sessions.get(p.getUniqueId());
        if (state == null) return;

        String title = e.getView().getTitle();
        
        if (title.equals("通報理由を選択")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = clicked.getItemMeta().getDisplayName();
            state.reason = name;

            if (name.equals("破壊(荒らし行為)")) {
                state.isTransitioning = true;
                p.closeInventory();
                openDestructionCheckMenu(p);
                return;
            }

            if (name.equals("嫌がらせ")) {
                state.isTransitioning = true;
                p.closeInventory();
                state.waitingHarassmentDetail = true;
                p.sendMessage("§eどのような嫌がらせを受けましたか？チャット欄に入力してください。");
                return;
            }

            if (name.equals("その他")) {
                state.isTransitioning = true;
                p.closeInventory();
                state.waitingReasonText = true;
                p.sendMessage("§e通報内容をチャットで入力してください。");
                return;
            }

            state.isTransitioning = true;
            openPlayerMenu(p);
        }
        if (title.equals("破壊されているのは現在の場所ですか?")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = clicked.getItemMeta().getDisplayName();
            
            if (name.equals("はい")) {
                state.isTransitioning = true;
                p.closeInventory();
                openPlayerMenu(p);
            } else {
                p.closeInventory();
                p.sendMessage("§cその場所に移動してから再度通報してください。");
                sessions.remove(p.getUniqueId());
            }
        }


        if (title.equals("誰を通報しますか？")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = clicked.getItemMeta().getDisplayName();

            if (name.equals("ここにいない")) {
                state.isTransitioning = true;
                p.closeInventory();
                state.waitingPlayerName = true;
                p.sendMessage("§e通報するプレイヤーの名前を入力してください。わからない場合は適当な1文字を入力してください。");
                return;
            }

            state.target = name;
            p.closeInventory();
            finishReport(p);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        SessionState state = sessions.get(p.getUniqueId());
        if (state == null) return;

        if (state.isTransitioning) {
            state.isTransitioning = false;
            return;
        }

        sessions.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sessions.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        SessionState state = sessions.get(p.getUniqueId());
        if (state == null) return;

        if (state.waitingReasonText) {
            e.setCancelled(true);
            state.reasonDetail = e.getMessage();
            state.waitingReasonText = false;
            Bukkit.getScheduler().runTask(this, () -> openPlayerMenu(p));
            return;
        }

        if (state.waitingHarassmentDetail) {
            e.setCancelled(true);
            state.harassmentDetail = e.getMessage();
            state.waitingHarassmentDetail = false;

            Bukkit.getScheduler().runTask(this, () -> openPlayerMenu(p));
            return;
        }

        if (state.waitingPlayerName) {
            e.setCancelled(true);
            state.target = e.getMessage();
            state.waitingPlayerName = false;
            Bukkit.getScheduler().runTask(this, () -> finishReport(p));
        }
    }


    private void finishReport(Player p) {
        SessionState s = sessions.remove(p.getUniqueId());
        if (s == null) return;

        String coords = String.format("(X: %.1f Y: %.1f Z: %.1f)",
                p.getLocation().getX(),
                p.getLocation().getY(),
                p.getLocation().getZ());

        String content = "対象: " + s.target +
                "\n理由: " + s.reason +
                (s.reasonDetail != null ? " (" + s.reasonDetail + ")" : "") +
                (s.harassmentDetail != null ? "\n嫌がらせの詳細: " + s.harassmentDetail : "") +
                "\n座標: " + coords;


        webhookSender.sendReport(p.getName(), content);

        p.sendMessage("§a通報を送信しました。");
    }


    private ItemStack item(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        i.setItemMeta(im);
        return i;
    }

    private ItemStack targetHead(Player p) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.setDisplayName(p.getName());
        head.setItemMeta(sm);
        return head;
    }

    private static class SessionState {
        String reason;
        String reasonDetail;
        String harassmentDetail;
        String target;
        boolean waitingReasonText;
        boolean waitingPlayerName;
        boolean waitingHarassmentDetail;
        boolean isTransitioning;
    }

}