package ru.templetitles;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Bukkit;
// Removed ChatColor import as DataManager provides translated strings
import java.util.ArrayList; // Added for lore processing
import java.util.List; // Added for lore processing
// Removed Arrays import as it might not be needed if lore is processed differently

public class CustomTitulCommand implements CommandExecutor {
    private final TempleTitles plugin;
    private final DataManager dataManager; // Added DataManager instance

    public CustomTitulCommand(TempleTitles plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager(); // Initialize DataManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(dataManager.getMsgPlayerOnlyCommand()); // Use DataManager
            return true;
        }

        Player player = (Player) sender;
        
        Inventory gui = Bukkit.createInventory(null, 45, dataManager.getGuiMainMenuTitle()); // Use DataManager

        // Slot 20: View Your Titles
        ItemStack viewTitlesItem = new ItemStack(Material.BOOK);
        ItemMeta viewTitlesMeta = viewTitlesItem.getItemMeta();
        if (viewTitlesMeta != null) {
            viewTitlesMeta.setDisplayName(dataManager.getGuiMainMenuItemViewOwnedName()); // Use DataManager
            viewTitlesMeta.setLore(dataManager.getGuiMainMenuItemViewOwnedLore()); // Use DataManager (already translated List<String>)
            viewTitlesItem.setItemMeta(viewTitlesMeta);
        }
        gui.setItem(20, viewTitlesItem);

        // Slot 22: Request a New Title
        ItemStack requestTitleItem = new ItemStack(Material.PAPER);
        ItemMeta requestTitleMeta = requestTitleItem.getItemMeta();
        if (requestTitleMeta != null) {
            requestTitleMeta.setDisplayName(dataManager.getGuiMainMenuItemRequestName()); // Use DataManager
            
            List<String> requestLore = new ArrayList<>();
            String costString = String.valueOf(dataManager.getTitleRequestCost());
            for (String line : dataManager.getGuiMainMenuItemRequestLore()) {
                requestLore.add(line.replace("%cost%", costString));
            }
            // Check if player has enough tokens and append specific lore line if needed
            // This logic is better placed in GUIListener or when GUI is opened,
            // but if config has specific "no_tokens_lore" for the main item itself:
            if (dataManager.getPlayerTokens(player.getUniqueId()) < dataManager.getTitleRequestCost()) {
                 if (dataManager.getGuiMainMenuItemRequestNoTokensLore() != null && !dataManager.getGuiMainMenuItemRequestNoTokensLore().isEmpty()){
                    requestLore.add(dataManager.getGuiMainMenuItemRequestNoTokensLore());
                 }
            }
            requestTitleMeta.setLore(requestLore);
            requestTitleItem.setItemMeta(requestTitleMeta);
        }
        gui.setItem(22, requestTitleItem);
        
        player.openInventory(gui);

        return true;
    }
}
