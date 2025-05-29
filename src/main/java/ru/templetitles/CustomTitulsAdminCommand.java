package ru.templetitles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
// Removed Bukkit and OfflinePlayer as 'give' subcommand is gone
import org.bukkit.ChatColor; // Added for messages
import java.util.List; // Added for TitleRequest list

public class CustomTitulsAdminCommand implements CommandExecutor {
    private final TempleTitles plugin;
    private final DataManager dataManager;

    public CustomTitulsAdminCommand(TempleTitles plugin) { // Constructor updated, DataManager will be passed from main
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager(); // Get DataManager from plugin instance
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission templetitles.admin is checked by Bukkit via plugin.yml.

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players to open the GUI.");
            return true;
        }
        
        // If any args are provided, show usage, as only GUI opening is supported now.
        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /customtituls - Opens the title request management GUI.");
            return true;
        }

        Player admin = (Player) sender;
        List<TitleRequest> requests = dataManager.getPendingRequests();
        
        // Call the static method in AdminTitleGUI
        AdminTitleGUI.openAdminRequestsView(admin, requests, dataManager, plugin);
        
        return true;
    }
}
