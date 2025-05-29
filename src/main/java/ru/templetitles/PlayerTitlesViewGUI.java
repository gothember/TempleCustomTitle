package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Your Titles"); // 6 rows * 9 slots = 54

        for (int i = 0; i < titles.size(); i++) {
            if (i >= 54) break; // Stop if inventory is full

            PlayerTitle titleEntry = titles.get(i);
            ItemStack titleItem = new ItemStack(Material.PAPER);
            ItemMeta itemMeta = titleItem.getItemMeta();

            if (itemMeta != null) {
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f" + titleEntry.getTitle()));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Ваш титул")); // "Your title"
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Название титула: &f" + titleEntry.getTitle()));
                // Assuming approvalDate is already formatted. If not, formatting would be needed here.
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Одобрен: &e" + titleEntry.getApprovalDate()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Статус: &a" + titleEntry.getStatus())); // Assuming status is user-friendly
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Одобрен администратором: &6" + titleEntry.getAdminApproverName()));
                lore.add("");
                if ("одобрен".equalsIgnoreCase(titleEntry.getStatus())) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&6ЛКМ - экипировать титул"));
                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&cТитул не одобрен"));
                }
                itemMeta.setLore(lore);

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
