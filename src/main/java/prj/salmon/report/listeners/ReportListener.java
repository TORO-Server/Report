package prj.salmon.report.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import prj.salmon.report.managers.ReportManager;
import prj.salmon.report.utils.WebhookSender;

public class ReportListener implements Listener {
    private final ReportManager reportManager;
    private final JavaPlugin plugin;

    public ReportListener(JavaPlugin plugin, ReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ReportManager.SessionState state = reportManager.getSession(p.getUniqueId());
        if (state == null) return;

        Component title = e.getView().title();
        
        if (title.equals(Component.text("通報理由を選択"))) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = "";
            if (clicked.hasItemMeta()) {
                Component displayName = clicked.getItemMeta().displayName();
                if (displayName != null) {
                    name = PlainTextComponentSerializer.plainText().serialize(displayName);
                }
            }
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
        if (title.equals(Component.text("破壊されているのは現在の場所ですか?"))) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = "";
            if (clicked.hasItemMeta()) {
                Component displayName = clicked.getItemMeta().displayName();
                if (displayName != null) {
                    name = PlainTextComponentSerializer.plainText().serialize(displayName);
                }
            }
            
            if (name.equals("はい")) {
                state.isTransitioning = true;
                p.closeInventory();
                openPlayerMenu(p);
            } else {
                p.closeInventory();
                p.sendMessage("§cその場所に移動してから再度通報してください。");
                reportManager.endSession(p.getUniqueId());
            }
        }


        if (title.equals(Component.text("誰を通報しますか？"))) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;

            String name = "";
            if (clicked.hasItemMeta()) {
                Component displayName = clicked.getItemMeta().displayName();
                if (displayName != null) {
                    name = PlainTextComponentSerializer.plainText().serialize(displayName);
                }
            }

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
        ReportManager.SessionState state = reportManager.getSession(p.getUniqueId());
        if (state == null) return;

        if (state.isTransitioning) {
            state.isTransitioning = false;
            return;
        }

        reportManager.endSession(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        reportManager.endSession(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        ReportManager.SessionState state = reportManager.getSession(p.getUniqueId());
        if (state == null) return;

        if (state.waitingReasonText) {
            e.setCancelled(true);
            state.reasonDetail = PlainTextComponentSerializer.plainText().serialize(e.message());
            state.waitingReasonText = false;
            Bukkit.getScheduler().runTask(plugin, () -> openPlayerMenu(p));
            return;
        }

        if (state.waitingHarassmentDetail) {
            e.setCancelled(true);
            state.harassmentDetail = PlainTextComponentSerializer.plainText().serialize(e.message());
            state.waitingHarassmentDetail = false;

            Bukkit.getScheduler().runTask(plugin, () -> openPlayerMenu(p));
            return;
        }

        if (state.waitingPlayerName) {
            e.setCancelled(true);
            state.target = PlainTextComponentSerializer.plainText().serialize(e.message());
            state.waitingPlayerName = false;
            Bukkit.getScheduler().runTask(plugin, () -> finishReport(p));
        }
    }

    private void openPlayerMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("誰を通報しますか？"));

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            inv.setItem(slot++, targetHead(target));
        }

        inv.setItem(53, item(Material.YELLOW_CONCRETE, "ここにいない"));
        p.openInventory(inv);
    }

    private void openDestructionCheckMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("破壊されているのは現在の場所ですか?"));

        inv.setItem(3, item(Material.LIME_CONCRETE, "はい"));
        inv.setItem(5, item(Material.RED_CONCRETE, "いいえ"));

        p.openInventory(inv);
    }

    private void finishReport(Player p) {
        ReportManager.SessionState s = reportManager.getSession(p.getUniqueId());
        if (s == null) return;
        reportManager.endSession(p.getUniqueId());

        String coords = String.format("(X: %.1f Y: %.1f Z: %.1f)",
                p.getLocation().getX(),
                p.getLocation().getY(),
                p.getLocation().getZ());

        String detail = null;
        if (s.reasonDetail != null) {
            detail = s.reasonDetail;
        } else if (s.harassmentDetail != null) {
            detail = "嫌がらせの詳細: " + s.harassmentDetail;
        }

        WebhookSender sender = reportManager.getWebhookSender();
        if (sender != null) {
            sender.sendReportAsync(p.getName(), s.target, s.reason, detail, coords)
                .thenRun(() -> {
                     // Async callback if needed (e.g. logging)
                });
        }

        p.sendMessage("§a通報を送信しました。");
    }

    private ItemStack item(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.displayName(Component.text(name));
        i.setItemMeta(im);
        return i;
    }

    private ItemStack targetHead(Player p) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.displayName(Component.text(p.getName()));
        head.setItemMeta(sm);
        return head;
    }
}
