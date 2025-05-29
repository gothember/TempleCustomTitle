package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command."); // Standard Bukkit message can also be used via plugin.yml
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /tokens give <player> <amount>");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /tokens give <player> <amount>");
                return true;
            }

            String playerName = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a positive integer.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount specified.");
                return true;
            }

            OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (targetOfflinePlayer == null || (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline())) {
                 // dataManager.getMsgPlayerNotFound() might need Bukkit.getPlayerUniqueId(playerName) if it expects UUID
                 // For simplicity, let's use a direct message here or assume getMsgPlayerNotFound can take a string.
                String message = dataManager.getMsgPlayerNotFound().replace("%player%", playerName);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return true;
            }

            dataManager.addPlayerTokens(targetOfflinePlayer.getUniqueId(), amount);
            
            String sentMessage = dataManager.getMsgTokensSent()
                                    .replace("%amount%", String.valueOf(amount))
                                    .replace("%player%", targetOfflinePlayer.getName()); // Use targetOfflinePlayer.getName() to get correct capitalization
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sentMessage));

            if (targetOfflinePlayer.isOnline()) {
                Player targetOnlinePlayer = (Player) targetOfflinePlayer;
                String receivedMessage = dataManager.getMsgTokensReceived().replace("%amount%", String.valueOf(amount));
                targetOnlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', receivedMessage));
            }
            return true;
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Unknown sub-command. Usage: /tokens give <player> <amount>");
            return true;
        }
    }
}
