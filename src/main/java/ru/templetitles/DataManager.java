package ru.templetitles;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class DataManager {
    private final JavaPlugin plugin;
    private File pendingTitlesFile;
    private FileConfiguration pendingTitlesConfig;
    private File playerTitlesFile;
    private FileConfiguration playerTitlesConfig;

    // Placeholder for actual data structures
    private List<TitleRequest> pendingRequestsList = new ArrayList<>();
    private Map<UUID, PlayerTitle> playerTitlesMap = new HashMap<>();
    private Map<UUID, Integer> playerTokensMap = new HashMap<>(); // Added for token management

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
        loadPendingRequests();
        loadPlayerTitles();
    }

    private void setupFiles() {
        pendingTitlesFile = new File(plugin.getDataFolder(), "pending_titles.yml");
        playerTitlesFile = new File(plugin.getDataFolder(), "player_titles.yml");

        // Ensure plugin data folder exists (usually done in onEnable of main class, but good check)
        // The main class TempleTitles already does this, so this is a double check.
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            if (!pendingTitlesFile.exists()) {
                pendingTitlesFile.createNewFile();
                plugin.getLogger().info("Created empty pending_titles.yml");
            } else {
                plugin.getLogger().info("Loading existing pending_titles.yml");
            }
            if (!playerTitlesFile.exists()) {
                playerTitlesFile.createNewFile();
                plugin.getLogger().info("Created empty player_titles.yml");
            } else {
                plugin.getLogger().info("Loading existing player_titles.yml");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create data files: " + e.getMessage());
            // Consider if the plugin should disable itself or throw a runtime exception
        }
        
        // reloadConfigs() should still be called after this block
        reloadConfigs();
    }
    
    public void reloadConfigs() {
        pendingTitlesConfig = YamlConfiguration.loadConfiguration(pendingTitlesFile);
        playerTitlesConfig = YamlConfiguration.loadConfiguration(playerTitlesFile);
    }

    public void savePendingRequests() {
        try {
            pendingTitlesConfig.save(pendingTitlesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending titles: " + e.getMessage());
        }
    }

    public void savePlayerTitles() {
        try {
            playerTitlesConfig.save(playerTitlesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player titles: " + e.getMessage());
        }
    }
    

    @SuppressWarnings("unchecked")
    public void loadPendingRequests() {
        pendingRequestsList.clear();
        if (pendingTitlesConfig.contains("requests")) {
            List<Map<?, ?>> requestsData = pendingTitlesConfig.getMapList("requests");
            for (Map<?, ?> reqData : requestsData) {
                UUID playerUUID = UUID.fromString((String) reqData.get("uuid"));
                String playerName = (String) reqData.get("playerName");
                String title = (String) reqData.get("title");
                pendingRequestsList.add(new TitleRequest(playerUUID, playerName, title));
            }
        }
    }

    public void addPendingRequest(TitleRequest request) {
        pendingRequestsList.add(request);
        List<Map<String, Object>> requestsData = new ArrayList<>();
        for (TitleRequest req : pendingRequestsList) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uuid", req.getPlayerUUID().toString());
            reqMap.put("playerName", req.getPlayerName());
            reqMap.put("title", req.getTitle());
            requestsData.add(reqMap);
        }
        pendingTitlesConfig.set("requests", requestsData);
        savePendingRequests();
    }

    public void removePendingRequest(UUID playerUUID, String title) {
        pendingRequestsList.removeIf(req -> req.getPlayerUUID().equals(playerUUID) && req.getTitle().equals(title));
        // Re-save the updated list
        List<Map<String, Object>> requestsData = new ArrayList<>();
        for (TitleRequest req : pendingRequestsList) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("uuid", req.getPlayerUUID().toString());
            reqMap.put("playerName", req.getPlayerName());
            reqMap.put("title", req.getTitle());
            requestsData.add(reqMap);
        }
        pendingTitlesConfig.set("requests", requestsData);
        savePendingRequests();
    }

    public List<TitleRequest> getPendingRequests() {
        return new ArrayList<>(pendingRequestsList); // Return a copy
    }

    @SuppressWarnings("unchecked")
    public void loadPlayerTitles() {
        playerTitlesMap.clear();
        playerTokensMap.clear(); // Clear tokens map on reload
        if (playerTitlesConfig.contains("titles")) {
             for (String uuidString : playerTitlesConfig.getConfigurationSection("titles").getKeys(false)) {
                UUID playerUUID = UUID.fromString(uuidString);
                String path = "titles." + uuidString;
                String playerName = playerTitlesConfig.getString(path + ".playerName");
                String title = playerTitlesConfig.getString(path + ".title");
                String status = playerTitlesConfig.getString(path + ".status");
                String approvalDate = playerTitlesConfig.getString(path + ".approvalDate");
                playerTitlesMap.put(playerUUID, new PlayerTitle(playerUUID, playerName, title, status, approvalDate));
                
                // Load tokens
                int tokens = playerTitlesConfig.getInt(path + ".tokens", 0);
                playerTokensMap.put(playerUUID, tokens);
            }
        }
    }

    public void addPlayerTitle(PlayerTitle playerTitle) {
        playerTitlesMap.put(playerTitle.getPlayerUUID(), playerTitle);
        String path = "titles." + playerTitle.getPlayerUUID().toString();
        playerTitlesConfig.set(path + ".playerName", playerTitle.getPlayerName());
        playerTitlesConfig.set(path + ".title", playerTitle.getTitle());
        playerTitlesConfig.set(path + ".status", playerTitle.getStatus());
        playerTitlesConfig.set(path + ".approvalDate", playerTitle.getApprovalDate());
        // Save tokens along with title, ensuring it uses the map's current value or default
        playerTitlesConfig.set(path + ".tokens", playerTokensMap.getOrDefault(playerTitle.getPlayerUUID(), 0));
        savePlayerTitles();
    }
    
    public void removePlayerTitle(UUID playerUUID) {
        playerTitlesMap.remove(playerUUID);
        playerTokensMap.remove(playerUUID); // Also remove from tokens map
        playerTitlesConfig.set("titles." + playerUUID.toString(), null); // Remove the section
        savePlayerTitles();
    }

    public PlayerTitle getPlayerTitle(UUID playerUUID) {
        return playerTitlesMap.get(playerUUID);
    }

    // Token Management Methods
    public int getPlayerTokens(UUID playerUUID) {
        return playerTokensMap.getOrDefault(playerUUID, 0);
    }

    public void setPlayerTokens(UUID playerUUID, int amount) {
        int newAmount = Math.max(0, amount); // Ensure tokens don't go negative
        playerTokensMap.put(playerUUID, newAmount);
        // Ensure the player's section exists if we are setting tokens,
        // otherwise, this might do nothing if they don't have a title entry yet.
        // This is generally fine as tokens are associated with players who interact with titles.
        playerTitlesConfig.set("titles." + playerUUID.toString() + ".tokens", newAmount);
        savePlayerTitles(); // Save immediately
    }

    public void addPlayerTokens(UUID playerUUID, int amountToAdd) {
        if (amountToAdd <= 0) return;
        int currentTokens = getPlayerTokens(playerUUID);
        setPlayerTokens(playerUUID, currentTokens + amountToAdd);
    }

    public boolean removePlayerTokens(UUID playerUUID, int amountToRemove) {
        if (amountToRemove <= 0) return true; // Removing nothing or negative is a "success"
        int currentTokens = getPlayerTokens(playerUUID);
        if (currentTokens >= amountToRemove) {
            setPlayerTokens(playerUUID, currentTokens - amountToRemove);
            return true;
        } else {
            return false; // Insufficient tokens
        }
    }
    
    public String getCurrentFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return formatter.format(new Date());
    }
}
