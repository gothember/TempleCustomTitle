package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class GUIListener implements Listener {
    private final TempleTitles plugin;
    private final DataManager dataManager;
    private final TitleInputManager titleInputManager;

    public GUIListener(TempleTitles plugin, DataManager dataManager, TitleInputManager titleInputManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.titleInputManager = titleInputManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof PlayerTitleGUI) {
            event.setCancelled(true); // Prevent item taking
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            int slot = event.getRawSlot(); // Use raw slot for GUI top inventory

            if (slot == 0 && clickedItem.getType() == Material.PAPER) { // Equip Title
                PlayerTitle playerTitle = dataManager.getPlayerTitle(player.getUniqueId());
                if (playerTitle != null && "одобрен".equalsIgnoreCase(playerTitle.getStatus())) {
                    String commandToRun = "lp user " + player.getName() + " meta setsuffix 1 \"" + playerTitle.getTitle() + "\""; // Priority 1 for suffix
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                    player.sendMessage("§aТитул '" + playerTitle.getTitle() + "' успешно установлен!");
                    player.closeInventory();
                }
            } else if (slot == 22 && clickedItem.getType() == Material.PAPER) { // Request Title
                int tokenBalance = dataManager.getPlayerTokens(player.getUniqueId());
                if (tokenBalance < 1) {
                    player.sendMessage("§cУ вас недостаточно жетонов для запроса титула. Требуется 1 жетон.");
                    player.closeInventory(); // Close GUI as the action cannot proceed
                } else {
                    // Sufficient tokens, proceed with request
                    titleInputManager.addPlayerWaiting(player.getUniqueId());
                    player.sendMessage("§eВведите желаемый титул в чат.");
                    // Optional: add a timeout for input using BukkitScheduler
                    player.closeInventory();
                }
            }

        } else if (event.getInventory().getHolder() instanceof AdminTitleGUI) {
            event.setCancelled(true); // Prevent item taking
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() != Material.PAPER) return;

            List<String> lore = clickedItem.getItemMeta().getLore();
            if (lore == null || lore.size() < 3) return; // Basic check for enough lore lines

            String targetPlayerName = null;
            String requestedTitle = null;
            UUID targetPlayerUUID = null;

            for (String line : lore) {
                if (line.startsWith(AdminTitleGUI.LORE_PLAYER_NAME_PREFIX)) {
                    targetPlayerName = line.substring(AdminTitleGUI.LORE_PLAYER_NAME_PREFIX.length());
                } else if (line.startsWith(AdminTitleGUI.LORE_TITLE_PREFIX)) {
                    requestedTitle = line.substring(AdminTitleGUI.LORE_TITLE_PREFIX.length());
                } else if (line.startsWith(AdminTitleGUI.LORE_PLAYER_UUID_PREFIX)) {
                    try {
                        targetPlayerUUID = UUID.fromString(line.substring(AdminTitleGUI.LORE_PLAYER_UUID_PREFIX.length()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Could not parse UUID from admin GUI lore: " + line);
                        return;
                    }
                }
            }
            
            if (targetPlayerName == null || requestedTitle == null || targetPlayerUUID == null) {
                 plugin.getLogger().warning("Could not parse all required info from admin GUI item.");
                 return;
            }

            Player admin = player; // Admin is the one clicking

            if (event.isLeftClick()) { // Approve
                dataManager.removePendingRequest(targetPlayerUUID, requestedTitle);
                PlayerTitle newTitle = new PlayerTitle(targetPlayerUUID, targetPlayerName, requestedTitle, "одобрен", dataManager.getCurrentFormattedDate());
                dataManager.addPlayerTitle(newTitle);
                admin.sendMessage("§aТитул для " + targetPlayerName + " ('" + requestedTitle + "') одобрен.");
                
                Player onlineTargetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                if (onlineTargetPlayer != null) { // Removed isOnline() check, Bukkit.getPlayer already checks this.
                    onlineTargetPlayer.sendMessage("§aВаш титул '" + requestedTitle + "' был одобрен администратором!");
                }
                // Refresh admin GUI by creating a new one and opening it
                admin.openInventory(new AdminTitleGUI(plugin).getInventory()); // Refresh
            } else if (event.isRightClick()) { // Deny
                dataManager.removePendingRequest(targetPlayerUUID, requestedTitle);
                admin.sendMessage("§cТитул для " + targetPlayerName + " ('" + requestedTitle + "') отклонен.");

                Player onlineTargetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                if (onlineTargetPlayer != null) { // Removed isOnline() check
                    onlineTargetPlayer.sendMessage("§cВаш титул '" + requestedTitle + "' был отклонен администратором.");
                }
                // Refresh admin GUI
                admin.openInventory(new AdminTitleGUI(plugin).getInventory()); // Refresh
            }
            // Removed the final else-if block as it's redundant; individual GUI sections already cancel.
        }
    }
}
