package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
// import org.bukkit.inventory.meta.ItemMeta; // No longer directly needed
import java.util.ArrayList;
// import java.util.Arrays; // No longer directly needed
import java.util.List;

public class PlayerTitleGUI implements InventoryHolder {
    private final Inventory inventory;
    private final TempleTitles plugin;
    private final Player player;
    private final DataManager dataManager;

    public PlayerTitleGUI(TempleTitles plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.dataManager = plugin.getDataManager();
        this.inventory = Bukkit.createInventory(this, 45, "Кастомный титул"); // 5 rows * 9 slots
        initializeItems();
    }

    private void initializeItems() {
        PlayerTitle approvedTitle = dataManager.getPlayerTitle(player.getUniqueId());

        if (approvedTitle != null && "одобрен".equalsIgnoreCase(approvedTitle.getStatus())) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Титул: §f" + approvedTitle.getTitle());
            lore.add("§7Статус: §a" + approvedTitle.getStatus());
            lore.add("§7Дата выдачи: §e" + approvedTitle.getApprovalDate());
            lore.add("");
            lore.add("§6ЛКМ - Установить титул");
            inventory.setItem(0, Util.createGuiItem(Material.PAPER, "§bВаш одобренный титул", lore));
        } else {
            inventory.setItem(22, Util.createGuiItem(Material.PAPER, "§aЗапросить новый титул",
                "§7Нажмите здесь, чтобы подать заявку",
                "§7на создание кастомного титула.",
                "§7Стоимость: 1 Жетон" // Added token cost to lore
            ));
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
