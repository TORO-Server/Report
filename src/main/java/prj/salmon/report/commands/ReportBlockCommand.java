package prj.salmon.report.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import prj.salmon.report.managers.ReportManager;

public class ReportBlockCommand implements CommandExecutor {
    private final ReportManager reportManager;

    public ReportBlockCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§e使用法: /reportblock <プレイヤー名>");
            return true;
        }

        String target = args[0];
        if (reportManager.isBlocked(target)) {
            reportManager.setBlocked(target, false);
            sender.sendMessage("§a" + target + " の通報制限を解除しました。");
        } else {
            reportManager.setBlocked(target, true);
            sender.sendMessage("§c" + target + " からの通報をブロックしました。");
        }

        return true;
    }
}
