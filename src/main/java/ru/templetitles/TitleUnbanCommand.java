package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID; // Added for UUID

public class TitleUnbanCommand implements CommandExecutor {

    private final TempleTitles plugin;
    private final DataManager dataManager;

    public TitleUnbanCommand(TempleTitles plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("templetitles.unban")) {
            sender.sendMessage(dataManager.getMsgPermissionDenied());
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(dataManager.getMsgUsageTitleUnban()); // Fetched from DataManager
            return true;
        }

        String targetName = args[0];
        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);

        // Similar check as in TitleBanCommand
        if (targetOfflinePlayer == null || !targetOfflinePlayer.hasPlayedBefore()) {
            sender.sendMessage(dataManager.getMsgPlayerNeverPlayed().replace("%player%", targetName));
            return true;
        }
        UUID targetUUID = targetOfflinePlayer.getUniqueId();
        String actualTargetName = targetOfflinePlayer.getName() != null ? targetOfflinePlayer.getName() : targetName;


        // isPlayerBannedFromTitleCreation performs lazy cleanup of expired bans.
        // So, if it returns false, and getBanExpiryTimestamp is 0, they were not actively banned or ban just expired.
        boolean wasActivelyBanned = dataManager.isPlayerBannedFromTitleCreation(targetUUID);
        if (!wasActivelyBanned && dataManager.getBanExpiryTimestamp(targetUUID) == 0L) {
            sender.sendMessage(dataManager.getMsgPlayerNotBanned().replace("%player_name%", actualTargetName));
            return true;
        }

        dataManager.unbanPlayerTitleCreation(targetUUID);

        sender.sendMessage(dataManager.getMsgTitleUnbanSuccessAdmin()
                .replace("%player_name%", actualTargetName));

        if (targetOfflinePlayer.isOnline()) {
            Player targetOnlinePlayer = targetOfflinePlayer.getPlayer(); // Safe if isOnline is true
            if (targetOnlinePlayer != null) {
                 targetOnlinePlayer.sendMessage(dataManager.getMsgTitleUnbanNotificationPlayer()
                    .replace("%admin_name%", sender.getName()));
            }
        }
        return true;
    }
}
