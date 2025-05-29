package ru.templetitles;

import org.bukkit.Bukkit;
// Removed ChatColor import as DataManager provides translated strings
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PlayerTitlesViewGUI { // No longer implements InventoryHolder

    // Private constructor to prevent instantiation of this utility class
    private PlayerTitlesViewGUI() {}

    public static void openPlayerTitlesView(Player player, List<PlayerTitle> titles, DataManager dataManager, TempleTitles plugin) {
        Inventory gui = Bukkit.createInventory(null, 54, dataManager.getGuiPlayerTitlesViewTitle()); // Use DataManager

        for (int i = 0; i < titles.size(); i++) {
            if (i >= 54) break; // Stop if inventory is full

            PlayerTitle titleEntry = titles.get(i);
            ItemStack titleItem = new ItemStack(Material.PAPER);
            ItemMeta itemMeta = titleItem.getItemMeta();

            if (itemMeta != null) {
                // Translate the player-defined title name for display
                String rawPlayerTitle = titleEntry.getTitle();
                String translatedPlayerTitleForDisplay = Util.translateColors(dataManager.getGuiPlayerTitlesViewItemNamePrefix() + rawPlayerTitle);
                itemMeta.setDisplayName(translatedPlayerTitleForDisplay);

                List<String> processedLore = new ArrayList<>();
                String statusText;
                String titleStatus = titleEntry.getStatus(); // Assuming status is already in a comparable format (e.g. "одобрен")
                                
                // Determine status text from DataManager based on the title's status field
                if (dataManager.getTitleStatusApprovedText().contains(titleStatus) || "одобрен".equalsIgnoreCase(titleStatus)) { // Example check
                    statusText = dataManager.getTitleStatusApprovedText();
                } else if (dataManager.getTitleStatusRejectedText().contains(titleStatus) || "отклонен".equalsIgnoreCase(titleStatus)) {
                    statusText = dataManager.getTitleStatusRejectedText();
                } else { // Default to pending or a generic status if not explicitly matched
                    statusText = dataManager.getTitleStatusPendingText(); 
                }

                // Base Lore
                // Translate player title for lore placeholder replacement
                String translatedPlayerTitleForLore = Util.translateColors(rawPlayerTitle); 
                for (String line : dataManager.getGuiPlayerTitlesViewItemLoreBase()) {
                    processedLore.add(line
                        .replace("%titul_name%", translatedPlayerTitleForLore)
                        // No other common placeholders typically in base, but can be added
                    );
                }
                
                // Status Line
                for (String line : dataManager.getGuiPlayerTitlesViewItemLoreStatusLine()) {
                    processedLore.add(line.replace("%status%", statusText));
                }

                // Admin Approver Line (only if admin approver name exists)
                if (titleEntry.getAdminApproverName() != null && !titleEntry.getAdminApproverName().isEmpty()) {
                    for (String line : dataManager.getGuiPlayerTitlesViewItemLoreAdminLine()) {
                        processedLore.add(line.replace("%admin_name%", titleEntry.getAdminApproverName()));
                    }
                }
                
                // Approval Date Line (only if approval date exists)
                if (titleEntry.getApprovalDate() != null && !titleEntry.getApprovalDate().isEmpty()) {
                    for (String line : dataManager.getGuiPlayerTitlesViewItemLoreDateLine()) {
                        processedLore.add(line.replace("%date%", titleEntry.getApprovalDate()));
                    }
                }

                // Instructions based on status
                if ("одобрен".equalsIgnoreCase(titleStatus)) { // Using the raw status for logic here
                    processedLore.addAll(dataManager.getGuiPlayerTitlesViewItemLoreEquipInstruction());
                } else {
                    processedLore.addAll(dataManager.getGuiPlayerTitlesViewItemLoreNotApprovedInstruction());
                }
                itemMeta.setLore(processedLore);

                // Store the raw title name in PersistentDataContainer
                NamespacedKey namespacedKey = new NamespacedKey(plugin, "title_name");
                itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, titleEntry.getTitle());

                titleItem.setItemMeta(itemMeta);
                gui.setItem(i, titleItem); // Add item to the GUI
            }
        }
        player.openInventory(gui);
    }
}
