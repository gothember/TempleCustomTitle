package ru.templetitles;

import java.util.UUID;

public class PlayerTitle {
    private final UUID playerUUID;
    private final String playerName;
    private final String title;
    private final String status; // e.g., "одобрен"
    private final String approvalDate; // Formatted date string

    public PlayerTitle(UUID playerUUID, String playerName, String title, String status, String approvalDate) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.title = title;
        this.status = status;
        this.approvalDate = approvalDate;
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

    public String getStatus() {
        return status;
    }

    public String getApprovalDate() {
        return approvalDate;
    }
}
