package prj.salmon.report.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import prj.salmon.report.managers.ReportManager;

public class ReportCommand implements CommandExecutor {
    private final ReportManager reportManager;

    public ReportCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (reportManager.isBlocked(p.getName())) {
            p.sendMessage("§cあなたは通報を使用できません。");
            return true;
        }

        if (!reportManager.canReport(p.getUniqueId())) {
            p.sendMessage("§c連続しての通報は制限しています。");
            return true;
        }

        openReasonMenu(p);
        return true;
    }

    private void openReasonMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("通報理由を選択"));

        inv.setItem(11, item(Material.TNT, "破壊(荒らし行為)"));
        inv.setItem(13, item(Material.GRAY_CONCRETE_POWDER, "嫌がらせ"));
        inv.setItem(15, item(Material.YELLOW_CONCRETE, "その他"));

        reportManager.startSession(p.getUniqueId());
        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.displayName(Component.text(name));
        i.setItemMeta(im);
        return i;
    }
}
