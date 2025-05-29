package ru.templetitles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CustomTitulCommand implements CommandExecutor {
    private final TempleTitles plugin;
    private final DataManager dataManager;

    public CustomTitulCommand(TempleTitles plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков.");
            return true;
        }

        Player player = (Player) sender;
        PlayerTitle existingTitle = dataManager.getPlayerTitle(player.getUniqueId());
        
        // For now, we just create the GUI. Logic for what's in it will be in PlayerTitleGUI
        // and click handling will be in a listener.
        PlayerTitleGUI gui = new PlayerTitleGUI(plugin, player); // Pass plugin and player
        player.openInventory(gui.getInventory());

        return true;
    }
}
