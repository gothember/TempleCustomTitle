package ru.templetitles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final TitleInputManager titleInputManager;

    public PlayerQuitListener(TitleInputManager manager) {
        this.titleInputManager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        titleInputManager.stopTitleInput(event.getPlayer());
    }
}
