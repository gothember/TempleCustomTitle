package ru.templetitles;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TitleInputManager {
    private final Set<UUID> playersWaitingForTitleInput = new HashSet<>();

    public void addPlayerWaiting(UUID uuid) {
        playersWaitingForTitleInput.add(uuid);
    }

    public void removePlayerWaiting(UUID uuid) {
        playersWaitingForTitleInput.remove(uuid);
    }

    public boolean isPlayerWaiting(UUID uuid) {
        return playersWaitingForTitleInput.contains(uuid);
    }
}
