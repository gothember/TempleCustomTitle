package ru.templetitles;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
// Removed ChatColor import as DataManager provides translated strings
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Map; // Added for Map
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

        // Compare with titles from DataManager
        if (viewTitle.equals(dataManager.getGuiMainMenuTitle())) {
            event.setCancelled(true); // Cancel event for all clicks in this GUI

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            int clickedSlot = event.getSlot(); // Use getSlot() for raw slot index

            // Check if the clicked slot is a decoration slot and not a functional slot
            Map<Integer, DataManager.DecorationItemConfig> decorations = dataManager.getGuiMainMenuDecorations();
            if (decorations != null && decorations.containsKey(clickedSlot)) {
                // Ensure it's not a functional slot that might accidentally be in decoration range
                if (clickedSlot == 20 || clickedSlot == 22) {
                    // This is a functional slot, let subsequent logic handle it.
                } else {
                    DataManager.DecorationItemConfig decoConfig = decorations.get(clickedSlot);
                    if (decoConfig != null && decoConfig.getCommands() != null && !decoConfig.getCommands().isEmpty()) {
                        for (String command : decoConfig.getCommands()) {
                            String processedCommand = command.replace("%player_name%", player.getName())
                                                             .replace("%player_uuid%", player.getUniqueId().toString());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                        }
                        // player.closeInventory(); // Optional: GUI stays open by default
                        return; // Command executed, no further action for this click
                    }
                    // If decoConfig exists but has no commands, it remains purely decorative.
                    // The click is already cancelled by the general GUI handling.
                    return; // Explicitly return to signify decorative item was handled.
                }
            }

            // Assuming item names are also from DataManager for robustness, though not strictly required by task for click logic
            // String requestItemName = dataManager.getGuiMainMenuItemRequestName();
            // String viewOwnedItemName = dataManager.getGuiMainMenuItemViewOwnedName();

            // Logic based on slot as before, assuming fixed layout
            // int slot = event.getRawSlot(); // Already have clickedSlot
            if (clickedSlot == 22 && clickedItem.getType() == Material.PAPER) { // Request New Title
                int requiredTokens = dataManager.getTitleRequestCost();
                if (dataManager.getPlayerTokens(player.getUniqueId()) >= requiredTokens) {
                    dataManager.removePlayerTokens(player.getUniqueId(), requiredTokens);
                    player.closeInventory();
                    titleInputManager.startTitleInput(player);
                    player.sendMessage(dataManager.getMsgTitleInputPrompt()); // Use DataManager
                } else {
                    player.sendMessage(dataManager.getMsgInsufficientTokens().replace("%required_tokens%", String.valueOf(requiredTokens)));
                    player.closeInventory();
                }
            } else if (clickedSlot == 20 && clickedItem.getType() == Material.BOOK) { // View Titles
                List<PlayerTitle> playerTitles = dataManager.getPlayerTitles(player.getUniqueId());
                PlayerTitlesViewGUI.openPlayerTitlesView(player, playerTitles, dataManager, plugin);
            }
            // Other clicks in this GUI are implicitly handled by the decoration check or if they don't match slots 20/22

        } else if (viewTitle.equals(dataManager.getGuiPlayerTitlesViewTitle())) {
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
                    String rawTitle = foundTitle.getTitle();
                    // Translate title for messages and command
                    String translatedTitleForDisplayAndCommand = Util.translateColors(rawTitle);

                    // Using raw status "одобрен" for logic, but display status comes from getTitleStatusApprovedText()
                    if ("одобрен".equalsIgnoreCase(foundTitle.getStatus())) {
                        String titleWithLeadingSpace = " " + translatedTitleForDisplayAndCommand; // Add leading space
                        String command = String.format("lp user %s meta setsuffix %d \"%s\"",
                                                       player.getName(),
                                                       dataManager.getLuckpermsSuffixPriority(),
                                                       titleWithLeadingSpace); // Use title with leading space
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        player.sendMessage(dataManager.getMsgTitleEquipped().replace("%title%", translatedTitleForDisplayAndCommand));
                        player.closeInventory();
                    } else {
                        // Replace %title% placeholder in the message
                        player.sendMessage(dataManager.getMsgTitleNotApproved().replace("%title%", translatedTitleForDisplayAndCommand));
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage(dataManager.getMsgErrorGeneric()); // Use DataManager
                    player.closeInventory();
                }
            }
        } else if (viewTitle.equals(dataManager.getGuiAdminRequestsViewTitle())) { // Use DataManager
            event.setCancelled(true);
            Player admin = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            NamespacedKey titleKey = new NamespacedKey(plugin, "title_name");
            NamespacedKey uuidKey = new NamespacedKey(plugin, "requester_uuid");
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (!container.has(titleKey, PersistentDataType.STRING) || !container.has(uuidKey, PersistentDataType.STRING)) {
                return;
            }
            String rawTitleNameFromPDC = container.get(titleKey, PersistentDataType.STRING); // Renamed for clarity
            UUID requesterUUID = UUID.fromString(container.get(uuidKey, PersistentDataType.STRING));

            TitleRequest originalRequest = dataManager.getPendingRequests().stream()
                .filter(r -> r.getPlayerUUID().equals(requesterUUID) && r.getTitle().equals(rawTitleNameFromPDC)) // Compare with raw title from PDC
                .findFirst().orElse(null);

            if (originalRequest == null) {
                admin.sendMessage(dataManager.getMsgAdminRequestStale()); // Use DataManager
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin);
                return;
            }
            String requesterName = originalRequest.getPlayerName();
            // Using raw status "одобрен" for PlayerTitle internal status field
            String approvedStatus = "одобрен";
            String rawTitleForLogic = originalRequest.getTitle(); // Use the title from the request object

            // Translate title for messages
            String translatedTitleForMessage = Util.translateColors(rawTitleForLogic);

            if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) { // Approve
                PlayerTitle newPlayerTitle = new PlayerTitle(requesterUUID, requesterName, rawTitleForLogic, approvedStatus, dataManager.getCurrentFormattedDate(), admin.getName());
                dataManager.addPlayerTitle(newPlayerTitle);
                dataManager.removePendingRequest(requesterUUID, rawTitleForLogic);
                admin.sendMessage(dataManager.getMsgAdminTitleApprovedFeedback()
                                  .replace("%title%", translatedTitleForMessage)
                                  .replace("%player%", requesterName));

                Player requesterOnline = Bukkit.getPlayer(requesterUUID);
                if (requesterOnline != null) {
                    requesterOnline.sendMessage(dataManager.getMsgTitleApproved().replace("%title%", translatedTitleForMessage));
                }
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin);

            } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) { // Reject
                dataManager.removePendingRequest(requesterUUID, rawTitleForLogic);
                admin.sendMessage(dataManager.getMsgAdminTitleRejectedFeedback()
                                  .replace("%title%", translatedTitleForMessage)
                                  .replace("%player%", requesterName));

                Player requesterOnline = Bukkit.getPlayer(requesterUUID);
                if (requesterOnline != null) {
                    requesterOnline.sendMessage(dataManager.getMsgTitleRejected().replace("%title%", translatedTitleForMessage));
                }
                AdminTitleGUI.openAdminRequestsView(admin, dataManager.getPendingRequests(), dataManager, plugin);
            }
        }
    }
}
