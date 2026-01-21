package prj.salmon.report;

import org.bukkit.plugin.java.JavaPlugin;
import prj.salmon.report.commands.ReportBlockCommand;
import prj.salmon.report.commands.ReportCommand;
import prj.salmon.report.listeners.ReportListener;
import prj.salmon.report.managers.ReportManager;

public class Report extends JavaPlugin {
    private ReportManager reportManager;

    @Override
    public void onEnable() {
        // Initialize Manager
        this.reportManager = new ReportManager(this);

        // Register Commands
        getCommand("report").setExecutor(new ReportCommand(reportManager));
        getCommand("reportblock").setExecutor(new ReportBlockCommand(reportManager));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ReportListener(this, reportManager), this);
        
        getLogger().info("Report plugin has been enabled successfully.");
    }
}