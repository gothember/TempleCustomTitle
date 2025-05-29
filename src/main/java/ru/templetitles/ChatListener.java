package ru.templetitles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
// Removed ChatColor import as DataManager provides translated strings

public class ChatListener implements Listener {
    private final TempleTitles plugin; // plugin field might not be used, but good for consistency
    private final DataManager dataManager;
    private final TitleInputManager titleInputManager;

    public ChatListener(TempleTitles plugin, DataManager dataManager, TitleInputManager titleInputManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.titleInputManager = titleInputManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (titleInputManager.isPlayerInInputMode(player)) { // Using isPlayerInInputMode
            event.setCancelled(true);
            String titleName = event.getMessage(); // Renamed for clarity as per instructions

            // Basic validation (e.g., length, allowed characters - can be expanded)
            // This part can be kept or removed based on whether validation is desired here or elsewhere
            if (titleName.equalsIgnoreCase("cancel")) { // Handle "cancel" first
                titleInputManager.stopTitleInput(player);
                player.sendMessage(dataManager.getMsgTitleInputCancelled()); // Use DataManager
                // Token was already deducted in GUIListener, no refund logic here based on current flow.
                return;
            }

            int minLength = dataManager.getTitleMinLength();
            int maxLength = dataManager.getTitleMaxLength();

            if (titleName.length() < minLength) {
                player.sendMessage(dataManager.getMsgTitleInputTooShort().replace("%min_length%", String.valueOf(minLength))); // Use DataManager
                // Player remains in input mode to try again or type 'cancel'
                return; 
            }
            if (titleName.length() > maxLength) {
                player.sendMessage(dataManager.getMsgTitleInputTooLong().replace("%max_length%", String.valueOf(maxLength))); // Use DataManager
                // Player remains in input mode to try again or type 'cancel'
                return;
            }
            
            titleInputManager.stopTitleInput(player); // Stop input mode

            TitleRequest request = new TitleRequest(player.getUniqueId(), player.getName(), titleName, System.currentTimeMillis());
            dataManager.addPendingRequest(request);
            
            player.sendMessage(dataManager.getMsgRequestSubmitted().replace("%title%", titleName)); // Use DataManager (already translated)
            
            // Optional: Admin notification logic can be added here if desired.
            // Example: Bukkit.broadcast(dataManager.getMsgAdminNewRequestNotification().replace(...), "templetitles.admin.notify");
        }
    }
}
