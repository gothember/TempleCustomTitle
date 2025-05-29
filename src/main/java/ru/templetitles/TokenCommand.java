package ru.templetitles;

import org.bukkit.Bukkit;
// Removed ChatColor import as DataManager provides translated strings
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TokenCommand implements CommandExecutor {
    private final TempleTitles plugin;
    private final DataManager dataManager;

    public TokenCommand(TempleTitles plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("templetitles.tokens.give")) {
            sender.sendMessage(dataManager.getMsgPermissionDenied()); // Use DataManager
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(dataManager.getCmdTokensUsage()); // Use DataManager
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length != 3) {
                sender.sendMessage(dataManager.getCmdTokensUsage()); // Use DataManager
                return true;
            }

            String playerName = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(dataManager.getCmdTokensAmountPositive()); // Use DataManager
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(dataManager.getCmdTokensInvalidAmount()); // Use DataManager
                return true;
            }

            OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(playerName);
            // Using targetOfflinePlayer.getName() for %player% placeholder as it's more reliable for correct casing
            // and works even if player is offline (Bukkit.getOfflinePlayer(name) might return a player object with the exact name given).
            // However, if the player has never played before, getName() might be null.
            // A more robust way is to use args[1] (playerName) for the message if targetOfflinePlayer.getName() is null.
            String displayPlayerName = targetOfflinePlayer.getName() != null ? targetOfflinePlayer.getName() : playerName;

            if (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline()) { // Check if player exists
                sender.sendMessage(dataManager.getMsgPlayerNotFound().replace("%player%", displayPlayerName)); // Use DataManager
                return true;
            }
            
            dataManager.addPlayerTokens(targetOfflinePlayer.getUniqueId(), amount);
            
            sender.sendMessage(dataManager.getMsgTokensSent() // Use DataManager
                                    .replace("%amount%", String.valueOf(amount))
                                    .replace("%player%", displayPlayerName));

            if (targetOfflinePlayer.isOnline()) {
                Player targetOnlinePlayer = (Player) targetOfflinePlayer;
                targetOnlinePlayer.sendMessage(dataManager.getMsgTokensReceived().replace("%amount%", String.valueOf(amount))); // Use DataManager
            }
            return true;
        } else {
            sender.sendMessage(dataManager.getCmdTokensUnknownSubcommand()); // Use DataManager
            return true;
        }
    }
}
