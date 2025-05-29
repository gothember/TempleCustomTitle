package ru.templetitles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent; // Using Async

public class ChatListener implements Listener {
    private final TempleTitles plugin;
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
        if (titleInputManager.isPlayerWaiting(player.getUniqueId())) {
            event.setCancelled(true);
            String requestedTitle = event.getMessage();

            // Basic validation (e.g., length, allowed characters - can be expanded)
            if (requestedTitle.length() < 3 || requestedTitle.length() > 30) {
                player.sendMessage("§cНазвание титула должно быть от 3 до 30 символов.");
                // Optionally, don't remove from waiting, let them try again, or add a try limit.
                // titleInputManager.removePlayerWaiting(player.getUniqueId()); // Remove if one try only
                return; 
            }

            // Deduct token
            boolean deductionSuccess = dataManager.removePlayerTokens(player.getUniqueId(), 1);

            if (!deductionSuccess) {
                player.sendMessage("§cПроизошла ошибка при списании жетона. Заявка не была создана. Пожалуйста, попробуйте еще раз.");
                // Also ensure they are not stuck in a waiting state if token deduction fails right after passing GUI check
                titleInputManager.removePlayerWaiting(player.getUniqueId());
                return; // Stop processing if token deduction failed
            } else {
                // Original logic (to be executed if token deduction is successful):
                TitleRequest newRequest = new TitleRequest(player.getUniqueId(), player.getName(), requestedTitle);
                dataManager.addPendingRequest(newRequest);
                
                player.sendMessage("§aВаша заявка на титул '" + requestedTitle + "' подана на рассмотрение. 1 жетон был списан.");
                titleInputManager.removePlayerWaiting(player.getUniqueId());
            }
        }
    }
}
