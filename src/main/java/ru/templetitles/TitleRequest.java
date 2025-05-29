package ru.templetitles;

import java.util.UUID;

public class TitleRequest {
    private final UUID playerUUID;
    private final String playerName;
    private final String title;

    public TitleRequest(UUID playerUUID, String playerName, String title) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.title = title;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTitle() {
        return title;
    }
}
