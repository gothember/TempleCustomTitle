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
// UUID import is not strictly necessary here if not used directly, but good for context
// import java.util.UUID; 

public class AdminTitleGUI { // No longer implements InventoryHolder

    // Private constructor to prevent instantiation if it's a utility class
    private AdminTitleGUI() {}

    // Constants for lore prefixes are removed as PDC is used now.
    // Old constants for reference (if needed for migration or other parts of code):
    // public static final String LORE_PLAYER_NAME_PREFIX = "§7Заявка от: §f";
    // public static final String LORE_TITLE_PREFIX = "§7Титул: §f";
    // public static final String LORE_PLAYER_UUID_PREFIX = "§7UUID: §c";

    public static void openAdminRequestsView(Player admin, List<TitleRequest> requests, DataManager dataManager, TempleTitles plugin) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Title Requests");

        if (requests.isEmpty()) {
            ItemStack noRequestsItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRequestsItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No Active Requests");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "There are currently no pending title requests.");
                meta.setLore(lore);
                noRequestsItem.setItemMeta(meta);
            }
            inventory.setItem(22, noRequestsItem); // Center if possible
        } else {
            for (int i = 0; i < requests.size(); i++) {
                if (i >= 54) break; // Stop if inventory is full

                TitleRequest req = requests.get(i);
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bTitle: &f" + req.getTitle()));

                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Requester: " + ChatColor.WHITE + req.getPlayerName());
                    lore.add(ChatColor.GRAY + "Submitted: " + ChatColor.WHITE + dataManager.formatTimestamp(req.getSubmissionTimestamp()));
                    lore.add("");
                    lore.add(ChatColor.GREEN + "Right-Click to Approve");
                    lore.add(ChatColor.RED + "Shift + Right-Click to Reject");
                    meta.setLore(lore);

                    // PersistentDataContainer (PDC)
                    NamespacedKey titleKey = new NamespacedKey(plugin, "title_name");
                    NamespacedKey uuidKey = new NamespacedKey(plugin, "requester_uuid");
                    meta.getPersistentDataContainer().set(titleKey, PersistentDataType.STRING, req.getTitle());
                    meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, req.getPlayerUUID().toString());
                    
                    item.setItemMeta(meta);
                    inventory.setItem(i, item); // Add item to the GUI
                }
            }
        }
        admin.openInventory(inventory);
    }
}
