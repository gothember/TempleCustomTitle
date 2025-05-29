package ru.templetitles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Bukkit; // Added import
import org.bukkit.ChatColor; // Added import
import java.util.Arrays; // Added import

public class CustomTitulCommand implements CommandExecutor {
    private final TempleTitles plugin;
    // DataManager is not directly used here anymore, but plugin instance is kept for potential future use
    // or if other methods in this class needed it.

    public CustomTitulCommand(TempleTitles plugin) {
        this.plugin = plugin;
        // this.dataManager = plugin.getDataManager(); // Not strictly needed for this command's direct logic now
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is only for players.");
            return true;
        }

        Player player = (Player) sender;
        
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.DARK_PURPLE + "Custom Title Options");

        // Slot 20: View Your Titles
        ItemStack viewTitlesItem = new ItemStack(Material.BOOK);
        ItemMeta viewTitlesMeta = viewTitlesItem.getItemMeta();
        if (viewTitlesMeta != null) {
            viewTitlesMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aView Your Titles"));
            viewTitlesItem.setItemMeta(viewTitlesMeta);
        }
        gui.setItem(20, viewTitlesItem);

        // Slot 22: Request a New Title
        ItemStack requestTitleItem = new ItemStack(Material.PAPER);
        ItemMeta requestTitleMeta = requestTitleItem.getItemMeta();
        if (requestTitleMeta != null) {
            requestTitleMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bRequest a New Title"));
            requestTitleMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&7Cost: 1 Token")));
            requestTitleItem.setItemMeta(requestTitleMeta);
        }
        gui.setItem(22, requestTitleItem);
        
        player.openInventory(gui);

        return true;
    }
}
