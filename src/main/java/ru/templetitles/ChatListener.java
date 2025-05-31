package ru.templetitles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.util.List;
import java.util.regex.Pattern; // Added for Pattern

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

            String lowerCaseMessage = titleName.toLowerCase().trim();
            List<String> cancelKeywords = dataManager.getTitleInputCancelKeywords();

            if (cancelKeywords.contains(lowerCaseMessage)) {
                titleInputManager.stopTitleInput(player);
                player.sendMessage(dataManager.getMsgTitleInputCancelled()); // Message from config
                // Token was already deducted in GUIListener, no refund logic here based on current flow.
                return; // Important to return after handling cancellation
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
            // Removed duplicate maxLength check here

            // New Pattern Validation
            if (dataManager.isPatternValidationEnabled()) {
                List<Pattern> forbiddenPatterns = dataManager.getCompiledForbiddenPatterns();
                // titleName is the raw input here, which is what we want to validate
                for (Pattern pattern : forbiddenPatterns) {
                    if (pattern.matcher(titleName).find()) {
                        player.sendMessage(dataManager.getMsgTitlePatternViolation());
                        // Player remains in input mode
                        return;
                    }
                }
            }

            // New: Max Pending Requests Check
            if (dataManager.getPendingRequests().size() >= dataManager.getMaxPendingRequests()) {
                player.sendMessage(dataManager.getMsgMaxPendingRequestsReached());
                // Player remains in input mode
                return;
            }

            // All validations passed
            titleInputManager.stopTitleInput(player); // Stop input mode successfully

            TitleRequest request = new TitleRequest(player.getUniqueId(), player.getName(), titleName, System.currentTimeMillis());
            dataManager.addPendingRequest(request);

            player.sendMessage(dataManager.getMsgRequestSubmitted().replace("%title%", titleName)); // Use DataManager (already translated)

            // Optional: Admin notification logic can be added here if desired.
            // Example: Bukkit.broadcast(dataManager.getMsgAdminNewRequestNotification().replace(...), "templetitles.admin.notify");
        }
    }
}
