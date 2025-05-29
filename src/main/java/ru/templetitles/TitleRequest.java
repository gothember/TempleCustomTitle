package ru.templetitles;

import java.util.UUID;

public class TitleRequest {
    private final UUID playerUUID;
    private final String playerName;
    private final String title;
    private final long submissionTimestamp;

    public TitleRequest(UUID playerUUID, String playerName, String title, long submissionTimestamp) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.title = title;
        this.submissionTimestamp = submissionTimestamp; // New
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

    public long getSubmissionTimestamp() {
        return submissionTimestamp;
    }
}
