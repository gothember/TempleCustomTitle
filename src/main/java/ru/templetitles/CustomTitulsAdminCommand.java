package ru.templetitles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit; // Added
import org.bukkit.OfflinePlayer; // Added

public class CustomTitulsAdminCommand implements CommandExecutor {
    private final TempleTitles plugin;

    public CustomTitulsAdminCommand(TempleTitles plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission is checked by Bukkit via plugin.yml.

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эта команда только для игроков в данной форме (открытие GUI).");
                return true;
            }
            Player admin = (Player) sender;
            AdminTitleGUI adminGui = new AdminTitleGUI(plugin);
            admin.openInventory(adminGui.getInventory());
            return true;
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length != 3) {
                sender.sendMessage("§cИспользование: /customtituls give <игрок> <количество>");
                return true;
            }

            OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (targetOfflinePlayer == null || (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline())) {
                sender.sendMessage("§cИгрок " + args[1] + " не найден.");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверное количество жетонов. Укажите положительное число.");
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage("§cНеверное количество жетонов. Укажите положительное число.");
                return true;
            }

            DataManager dataManager = plugin.getDataManager();
            dataManager.addPlayerTokens(targetOfflinePlayer.getUniqueId(), amount);

            sender.sendMessage("§aВы успешно выдали " + amount + " жетонов игроку " + targetOfflinePlayer.getName() + ".");

            if (targetOfflinePlayer.isOnline()) {
                Player targetOnlinePlayer = targetOfflinePlayer.getPlayer();
                if (targetOnlinePlayer != null) { // Check if player object is actually obtainable
                    targetOnlinePlayer.sendMessage("§aВам было выдано " + amount + " жетонов администратором.");
                }
            }
            return true;
        } else {
            sender.sendMessage("§cНеизвестная подкоманда. Использование: /customtituls [give <игрок> <количество>]");
            return true;
        }
    }
}
