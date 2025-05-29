package ru.templetitles;

import org.bukkit.entity.Player; // Added import
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TitleInputManager {
    private final Set<UUID> playersInInputMode = new HashSet<>(); // Renamed and matches task

    public void startTitleInput(Player player) { // Method signature updated
        playersInInputMode.add(player.getUniqueId());
    }

    public void stopTitleInput(Player player) { // Method signature updated
        playersInInputMode.remove(player.getUniqueId());
    }

    public boolean isPlayerInInputMode(Player player) { // Method signature updated
        return playersInInputMode.contains(player.getUniqueId());
    }
}
