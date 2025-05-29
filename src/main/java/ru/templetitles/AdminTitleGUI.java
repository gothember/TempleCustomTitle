package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
// import org.bukkit.inventory.meta.ItemMeta; // No longer directly needed
import java.util.ArrayList;
import java.util.List;

public class AdminTitleGUI implements InventoryHolder {
    private final Inventory inventory;
    private final TempleTitles plugin;
    private final DataManager dataManager;

    // Standardized prefixes for reliable parsing
    public static final String LORE_PLAYER_NAME_PREFIX = "§7Заявка от: §f";
    public static final String LORE_TITLE_PREFIX = "§7Титул: §f";
    public static final String LORE_PLAYER_UUID_PREFIX = "§7UUID: §c"; // Clean prefix for UUID

    public AdminTitleGUI(TempleTitles plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.inventory = Bukkit.createInventory(this, 54, "Админ: Заявки на титулы"); // 6 rows
        initializeItems();
    }

    private void initializeItems() {
        List<TitleRequest> pendingRequests = dataManager.getPendingRequests();
        int slot = 0;
        for (TitleRequest request : pendingRequests) {
            if (slot >= 54) break;

            List<String> lore = new ArrayList<>();
            lore.add("§bНовая заявка на кастомный титул");
            lore.add(PLAYER_NAME_PREFIX + request.getPlayerName());
            lore.add(TITLE_NAME_PREFIX + request.getTitle());
            lore.add(PLAYER_UUID_PREFIX + request.getPlayerUUID().toString()); // Store UUID
            lore.add("");
            lore.add("§aЛКМ - Принять заявку");
            lore.add("§cПКМ - Отклонить заявку");

            inventory.setItem(slot++, Util.createGuiItem(Material.PAPER, "§eЗаявка: " + request.getTitle(), lore));
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
