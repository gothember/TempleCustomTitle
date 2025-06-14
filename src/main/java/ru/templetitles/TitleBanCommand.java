package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID; // Added for UUID

// No need for ChatColor here if messages from DataManager are pre-translated

public class TitleBanCommand implements CommandExecutor {

    private final TempleTitles plugin; // For accessing DataManager or other plugin resources if needed
    private final DataManager dataManager;

    public TitleBanCommand(TempleTitles plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("templetitles.ban")) {
            sender.sendMessage(dataManager.getMsgPermissionDenied()); // Message from DataManager
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(dataManager.getMsgUsageTitleBan()); // Message from DataManager
            return true;
        }

        String targetName = args[0];
        String timeString = args[1];

        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);
        // For Bukkit.getOfflinePlayer(name), if the player has never played, it still returns an OfflinePlayer object.
        // The check targetOfflinePlayer.hasPlayedBefore() is crucial.
        // isOnline() is true if they are online, false otherwise.
        // A player might be online but hasPlayedBefore() is true.
        // A player might be offline but hasPlayedBefore() is true.
        // A player might be "online" (e.g. via Bungee) but hasPlayedBefore() is false on this specific server.
        // The most reliable check for "never truly been on this server" is !targetOfflinePlayer.hasPlayedBefore()
        if (targetOfflinePlayer == null || !targetOfflinePlayer.hasPlayedBefore()) {
             // Also check if targetOfflinePlayer.getUniqueId() is null, though it shouldn't be if hasPlayedBefore is true.
             // If a player is online, hasPlayedBefore should be true.
             // If targetOfflinePlayer.getName() is null, it means the player data couldn't be resolved at all for that name.
            sender.sendMessage(dataManager.getMsgPlayerNeverPlayed().replace("%player%", targetName));
            return true;
        }
        UUID targetUUID = targetOfflinePlayer.getUniqueId();

        long durationMillis = TimeUtil.parseDuration(timeString);
        if (durationMillis <= 0) {
            sender.sendMessage(dataManager.getMsgInvalidTimeFormat()); // Message from DataManager
            return true;
        }

        long expiryTimestamp = System.currentTimeMillis() + durationMillis;
        dataManager.banPlayerTitleCreation(targetUUID, expiryTimestamp);

        String formattedDuration = TimeUtil.formatDuration(durationMillis);
        String actualTargetName = targetOfflinePlayer.getName() != null ? targetOfflinePlayer.getName() : targetName;


        // Admin feedback
        sender.sendMessage(dataManager.getMsgTitleBanAppliedAdmin()
                .replace("%player_name%", actualTargetName)
                .replace("%duration%", formattedDuration));

        // Player notification if online
        if (targetOfflinePlayer.isOnline()) {
            Player targetOnlinePlayer = targetOfflinePlayer.getPlayer(); // Safe to cast if isOnline() is true
            if (targetOnlinePlayer != null) { // Double check if player object is obtainable
                 targetOnlinePlayer.sendMessage(dataManager.getMsgTitleBanAppliedPlayer()
                    .replace("%admin_name%", sender.getName())
                    .replace("%duration%", formattedDuration));
            }
        }
        return true;
    }
}
