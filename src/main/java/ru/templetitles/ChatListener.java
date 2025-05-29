package ru.templetitles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor; // Added import

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
            if (titleName.length() < 3 || titleName.length() > 30) {
                player.sendMessage(ChatColor.RED + "Title must be between 3 and 30 characters.");
                // Note: Player is still in input mode. They can try again or type 'cancel'.
                // Or, call titleInputManager.stopTitleInput(player); if it's a one-shot.
                // For this implementation, let's assume they need to type a valid title or 'cancel'.
                // If 'cancel' functionality is desired, it should be explicitly handled:
                if (titleName.equalsIgnoreCase("cancel")) {
                    titleInputManager.stopTitleInput(player);
                    player.sendMessage(ChatColor.YELLOW + "Title request cancelled.");
                    // Refund token if it was already deducted. In this flow, token is deducted *before* input.
                    // The current task description deducts token in GUIListener *before* starting input.
                    // So if they cancel here, the token is already spent.
                    // To make 'cancel' refund a token, token deduction should happen *after* successful title input.
                    // For now, sticking to the provided flow: token is spent once input mode starts.
                }
                return; 
            }
            
            titleInputManager.stopTitleInput(player); // Stop input mode

            TitleRequest request = new TitleRequest(player.getUniqueId(), player.getName(), titleName, System.currentTimeMillis());
            dataManager.addPendingRequest(request);
            
            String message = dataManager.getMsgRequestSubmitted().replace("%title%", titleName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            // Optional: Admin notification logic can be added here if desired.
            // Example: Bukkit.broadcast(ChatColor.AQUA + "New title request: " + titleName + " by " + player.getName(), "templetitles.admin.notify");
        }
    }
}
