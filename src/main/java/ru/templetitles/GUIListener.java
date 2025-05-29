package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta; // Added
import org.bukkit.persistence.PersistentDataType; // Added
import org.bukkit.ChatColor; // Added
import org.bukkit.NamespacedKey; // Added

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
        String viewTitle = event.getView().getTitle();

        if (viewTitle.equals(ChatColor.DARK_PURPLE + "Custom Title Options")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            int slot = event.getRawSlot();

            if (slot == 22 && clickedItem.getType() == Material.PAPER) { // Request New Title
                if (dataManager.getPlayerTokens(player.getUniqueId()) >= 1) {
                    dataManager.removePlayerTokens(player.getUniqueId(), 1);
                    player.closeInventory();
                    titleInputManager.startTitleInput(player);
                    player.sendMessage(ChatColor.GREEN + "Please type your desired title in chat.");
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', dataManager.getMsgInsufficientTokens()));
                    player.closeInventory();
                }
            } else if (slot == 20 && clickedItem.getType() == Material.BOOK) { // View Titles
                List<PlayerTitle> playerTitles = dataManager.getPlayerTitles(player.getUniqueId());
                PlayerTitlesViewGUI.openPlayerTitlesView(player, playerTitles, dataManager, plugin);
            }

        } else if (viewTitle.equals(ChatColor.DARK_AQUA + "Your Titles")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() != Material.PAPER) return;

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (itemMeta == null) return;

            NamespacedKey key = new NamespacedKey(plugin, "title_name");
            if (itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String titleName = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                PlayerTitle foundTitle = null;
                for (PlayerTitle pt : dataManager.getPlayerTitles(player.getUniqueId())) {
                    if (pt.getTitle().equals(titleName)) {
                        foundTitle = pt;
                        break;
                    }
                }

                if (foundTitle != null) {
                    if ("одобрен".equalsIgnoreCase(foundTitle.getStatus())) {
                        String command = String.format("lp user %s meta setsuffix %d \"%s\"", 
                                                       player.getName(), 
                                                       dataManager.getLuckpermsSuffixPriority(), 
                                                       foundTitle.getTitle());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        player.sendMessage(ChatColor.GREEN + "Title equipped!");
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', dataManager.getMsgTitleNotApproved()));
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Error: Could not find title data."); // Should not happen
                    player.closeInventory();
                }
            }
        } else if (viewTitle.equals(ChatColor.DARK_AQUA + "Title Requests")) { // Admin GUI for title requests
            event.setCancelled(true);
            Player admin = (Player) event.getWhoClicked(); // Admin is the one clicking
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            NamespacedKey titleKey = new NamespacedKey(plugin, "title_name");
            NamespacedKey uuidKey = new NamespacedKey(plugin, "requester_uuid");
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (!container.has(titleKey, PersistentDataType.STRING) || !container.has(uuidKey, PersistentDataType.STRING)) {
                return; // Not a valid request item
            }
            String titleName = container.get(titleKey, PersistentDataType.STRING);
            UUID requesterUUID = UUID.fromString(container.get(uuidKey, PersistentDataType.STRING));

            TitleRequest originalRequest = dataManager.getPendingRequests().stream()
                .filter(r -> r.getPlayerUUID().equals(requesterUUID) && r.getTitle().equals(titleName))
                .findFirst().orElse(null);

            if (originalRequest == null) {
                admin.sendMessage(ChatColor.RED + "This request seems to be outdated or already processed.");
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin); // Refresh
                return;
            }
            String requesterName = originalRequest.getPlayerName();

            if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) { // Approve
                PlayerTitle newPlayerTitle = new PlayerTitle(requesterUUID, requesterName, titleName, "одобрен", dataManager.getCurrentFormattedDate(), admin.getName());
                dataManager.addPlayerTitle(newPlayerTitle);
                dataManager.removePendingRequest(requesterUUID, titleName);
                admin.sendMessage(ChatColor.GREEN + "Title '" + titleName + "' approved for " + requesterName);

                Player requesterOnline = Bukkit.getPlayer(requesterUUID);
                if (requesterOnline != null) {
                    requesterOnline.sendMessage(ChatColor.translateAlternateColorCodes('&', dataManager.getMsgTitleApproved().replace("%title%", titleName)));
                }
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin); // Refresh GUI

            } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) { // Reject
                dataManager.removePendingRequest(requesterUUID, titleName);
                admin.sendMessage(ChatColor.RED + "Title '" + titleName + "' rejected for " + requesterName);

                Player requesterOnline = Bukkit.getPlayer(requesterUUID);
                if (requesterOnline != null) {
                    requesterOnline.sendMessage(ChatColor.translateAlternateColorCodes('&', dataManager.getMsgTitleRejected().replace("%title%", titleName)));
                }
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin); // Refresh GUI
            }
        }
    }
}
