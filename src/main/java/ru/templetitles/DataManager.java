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
// java.io.File is already imported above
import org.bukkit.ChatColor; // Needed for the old translateStringList, will be removed if Util is used. Or keep if used elsewhere.
// For Util class:
// import ru.templetitles.Util; // This will be used.
import org.bukkit.inventory.ItemStack; // Added
import org.bukkit.Material; // Added
import org.bukkit.inventory.meta.ItemMeta; // Added
import org.bukkit.configuration.ConfigurationSection; // Added

public class DataManager {
    private final JavaPlugin plugin;
    private String timeFormat;
    private int luckpermsSuffixPriority;
    private String msgTitleNotApproved;
    private String msgInsufficientTokens;
    private String msgTokensReceived;
    private String msgTokensSent;
    private String msgPlayerNotFound;
    private String msgRequestSubmitted;
    private String msgTitleApproved;
    private String msgTitleRejected;

    // General messages (new ones)
    private String msgPlayerOnlyCommand;
    private String msgPermissionDenied;
    private String msgTitleInputPrompt;
    private String msgTitleInputTooShort;
    private String msgTitleInputTooLong;
    private String msgTitleInputCancelled;
    private String msgTitleEquipped;
    private String msgErrorGeneric; // For errors like PDS data missing
    private String msgAdminRequestStale;
    private String msgAdminTitleApprovedFeedback; // For admin, e.g., "Title '%title%' approved for %player%."
    private String msgAdminTitleRejectedFeedback; // For admin, e.g., "Title '%title%' rejected for %player%."

    // GUI: main_menu
    private String guiMainMenuTitle;
    private String guiMainMenuItemRequestName;
    private List<String> guiMainMenuItemRequestLore;
    private String guiMainMenuItemRequestNoTokensLore; // Special lore line if not enough tokens
    private String guiMainMenuItemViewOwnedName;
    private List<String> guiMainMenuItemViewOwnedLore;
    private Map<Integer, ItemStack> guiMainMenuDecorations; // New field

    // GUI: player_titles_view
    private String guiPlayerTitlesViewTitle;
    private String guiPlayerTitlesViewItemNamePrefix; // e.g., "&f"
    private List<String> guiPlayerTitlesViewItemLoreBase; // Base lore applied to all titles
    private List<String> guiPlayerTitlesViewItemLoreStatusLine; // Format for "Status: %status%"
    private List<String> guiPlayerTitlesViewItemLoreAdminLine; // Format for "Approved by: %admin%"
    private List<String> guiPlayerTitlesViewItemLoreDateLine; // Format for "Approved on: %date%"
    private List<String> guiPlayerTitlesViewItemLoreEquipInstruction; // e.g., "LMB to equip"
    private List<String> guiPlayerTitlesViewItemLoreNotApprovedInstruction; // e.g., "Cannot equip (not approved)"

    // GUI: admin_requests_view
    private String guiAdminRequestsViewTitle;
    private String guiAdminRequestsViewNoRequestsItemName;
    private List<String> guiAdminRequestsViewNoRequestsItemLore;
    private String guiAdminRequestsViewRequestItemNamePrefix; // e.g., "&bTitle: &f"
    private List<String> guiAdminRequestsViewRequestItemLore; // Base lore, placeholders for player, submitted date, instructions

    // Commands: tokens
    private String cmdTokensUsage;
    private String cmdTokensAmountPositive;
    private String cmdTokensInvalidAmount;
    private String cmdTokensUnknownSubcommand;
    
    // Title Properties
    private int titleMinLength;
    private int titleMaxLength;
    private String titleStatusApprovedText; // Text for "одобрен" status in GUIs
    private String titleStatusRejectedText; // Text for "отклонен" status
    private String titleStatusPendingText;  // Text for "на рассмотрении" status
    private int titleRequestCost; // New: Cost in tokens to request a title

    private File pendingTitlesFile;
    private FileConfiguration pendingTitlesConfig;
    private File playerTitlesFile;
    private FileConfiguration playerTitlesConfig;

    // Placeholder for actual data structures
    private List<TitleRequest> pendingRequestsList = new ArrayList<>();
    private Map<UUID, List<PlayerTitle>> playerTitlesMap = new HashMap<>(); // Changed
    private Map<UUID, Integer> playerTokensMap = new HashMap<>(); // Added for token management

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigFile(); // Called before other initializations that might depend on config
        setupFiles();
        loadPendingRequests();
        loadPlayerTitles();
    }

    private void loadConfigFile() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // This relies on TempleTitles.onEnable() calling saveDefaultConfig() first
            // to ensure config.yml is in the data folder.
            // If it's still not there, it means saveDefaultConfig() in the main class failed
            // or was not called before DataManager instantiation.
            // For robustness, we can call it here if DataManager is instantiated
            // before the main class's onEnable fully sets up the config.
            // However, the standard practice is to ensure saveDefaultConfig() is called in onEnable()
            // of the main plugin class, prior to DataManager initialization.
             plugin.saveDefaultConfig(); // Ensure it's copied if missing
        }
        // It's important to reload the config after saveDefaultConfig might have run,
        // or if the config could have been changed externally.
        plugin.reloadConfig(); 

        timeFormat = plugin.getConfig().getString("messages.time_format", "dd.MM.yyyy HH:mm");
        luckpermsSuffixPriority = plugin.getConfig().getInt("luckperms.suffix_priority", 1);
        msgTitleNotApproved = plugin.getConfig().getString("messages.title_not_approved", "&cYour title is not yet approved.");
        msgInsufficientTokens = plugin.getConfig().getString("messages.insufficient_tokens", "&cYou do not have enough tokens.");
        msgTokensReceived = plugin.getConfig().getString("messages.tokens_received", "&aYou have received %amount% tokens.");
        msgTokensSent = plugin.getConfig().getString("messages.tokens_sent", "&aYou have sent %amount% tokens to %player%.");
        msgPlayerNotFound = plugin.getConfig().getString("messages.player_not_found", "&cPlayer %player% not found.");
        msgRequestSubmitted = plugin.getConfig().getString("messages.request_submitted", "&aYour title request for '%title%' has been submitted.");
        msgTitleApproved = Util.translateColors(plugin.getConfig().getString("messages.title_approved_player", "&aYour title '%title%' has been approved!"));
        msgTitleRejected = Util.translateColors(plugin.getConfig().getString("messages.title_rejected_player", "&cYour title '%title%' has been rejected."));

        // General Messages (New)
        msgPlayerOnlyCommand = Util.translateColors(plugin.getConfig().getString("messages.general.player_only_command", "&cThis command can only be run by a player."));
        msgPermissionDenied = Util.translateColors(plugin.getConfig().getString("messages.general.permission_denied", "&cYou do not have permission to use this command."));
        msgTitleInputPrompt = Util.translateColors(plugin.getConfig().getString("messages.general.title_input_prompt", "&aPlease type your desired title in chat. Type 'cancel' to abort."));
        msgTitleInputTooShort = Util.translateColors(plugin.getConfig().getString("messages.general.title_input_too_short", "&cTitle must be at least %min_length% characters."));
        msgTitleInputTooLong = Util.translateColors(plugin.getConfig().getString("messages.general.title_input_too_long", "&cTitle must be at most %max_length% characters."));
        msgTitleInputCancelled = Util.translateColors(plugin.getConfig().getString("messages.general.title_input_cancelled", "&eTitle request cancelled."));
        msgTitleEquipped = Util.translateColors(plugin.getConfig().getString("messages.general.title_equipped", "&aTitle '%title%' equipped!"));
        msgErrorGeneric = Util.translateColors(plugin.getConfig().getString("messages.general.error_generic", "&cAn unexpected error occurred. Please contact an administrator."));
        msgAdminRequestStale = Util.translateColors(plugin.getConfig().getString("messages.general.admin_request_stale", "&cThis request seems to be outdated or already processed."));
        msgAdminTitleApprovedFeedback = Util.translateColors(plugin.getConfig().getString("messages.general.admin_title_approved_feedback", "&aTitle '%title%' approved for player %player%."));
        msgAdminTitleRejectedFeedback = Util.translateColors(plugin.getConfig().getString("messages.general.admin_title_rejected_feedback", "&cTitle '%title%' rejected for player %player%."));
        
        // GUI: main_menu
        guiMainMenuTitle = Util.translateColors(plugin.getConfig().getString("gui.main_menu.title", "&5Custom Title Options"));
        guiMainMenuItemRequestName = Util.translateColors(plugin.getConfig().getString("gui.main_menu.items.request_title.name", "&bRequest a New Title"));
        guiMainMenuItemRequestLore = Util.translateStringList(plugin.getConfig().getStringList("gui.main_menu.items.request_title.lore"));
        guiMainMenuItemRequestNoTokensLore = Util.translateColors(plugin.getConfig().getString("gui.main_menu.items.request_title.no_tokens_lore", "&cNot enough tokens!"));
        guiMainMenuItemViewOwnedName = Util.translateColors(plugin.getConfig().getString("gui.main_menu.items.view_owned.name", "&aView Your Titles"));
        guiMainMenuItemViewOwnedLore = Util.translateStringList(plugin.getConfig().getStringList("gui.main_menu.items.view_owned.lore"));
        
        // Load GUI Main Menu Decorations
        guiMainMenuDecorations = new HashMap<>();
        ConfigurationSection decoSection = plugin.getConfig().getConfigurationSection("gui.main_menu.decorations");
        if (decoSection != null) {
            for (String slotKey : decoSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    if (slot < 0 || slot >= 45) { // Max slots for this GUI (5 rows * 9 columns)
                        plugin.getLogger().warning("[TempleTitles] Invalid slot number '" + slotKey + "' in gui.main_menu.decorations. Skipping.");
                        continue;
                    }

                    String materialName = decoSection.getString(slotKey + ".material");
                    String itemName = decoSection.getString(slotKey + ".name", " "); // Default to a space if name is missing

                    Material material = Material.matchMaterial(materialName);
                    if (material == null) {
                        plugin.getLogger().warning("[TempleTitles] Invalid material '" + materialName + "' for slot " + slot + " in gui.main_menu.decorations. Skipping.");
                        continue;
                    }

                    ItemStack itemStack = new ItemStack(material);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta != null) {
                        itemMeta.setDisplayName(Util.translateColors(itemName));
                        // itemMeta.setLore(new ArrayList<>()); // Optional: if you want to ensure no lore
                        // itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Optional: to hide attributes
                        itemStack.setItemMeta(itemMeta);
                    }
                    guiMainMenuDecorations.put(slot, itemStack);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[TempleTitles] Invalid slot key '" + slotKey + "' (must be an integer) in gui.main_menu.decorations. Skipping.");
                }
            }
        }

        // GUI: player_titles_view
        guiPlayerTitlesViewTitle = Util.translateColors(plugin.getConfig().getString("gui.player_titles_view.title", "&3Your Titles"));
        guiPlayerTitlesViewItemNamePrefix = Util.translateColors(plugin.getConfig().getString("gui.player_titles_view.item_name_prefix", "&f"));
        guiPlayerTitlesViewItemLoreBase = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.base"));
        guiPlayerTitlesViewItemLoreStatusLine = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.status_line"));
        guiPlayerTitlesViewItemLoreAdminLine = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.admin_line"));
        guiPlayerTitlesViewItemLoreDateLine = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.date_line"));
        guiPlayerTitlesViewItemLoreEquipInstruction = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.equip_instruction"));
        guiPlayerTitlesViewItemLoreNotApprovedInstruction = Util.translateStringList(plugin.getConfig().getStringList("gui.player_titles_view.item_lore.not_approved_instruction"));
        
        // GUI: admin_requests_view
        guiAdminRequestsViewTitle = Util.translateColors(plugin.getConfig().getString("gui.admin_requests_view.title", "&3Title Requests"));
        guiAdminRequestsViewNoRequestsItemName = Util.translateColors(plugin.getConfig().getString("gui.admin_requests_view.no_requests_item.name", "&cNo Active Requests"));
        guiAdminRequestsViewNoRequestsItemLore = Util.translateStringList(plugin.getConfig().getStringList("gui.admin_requests_view.no_requests_item.lore"));
        guiAdminRequestsViewRequestItemNamePrefix = Util.translateColors(plugin.getConfig().getString("gui.admin_requests_view.request_item.name_prefix", "&bTitle: &f"));
        guiAdminRequestsViewRequestItemLore = Util.translateStringList(plugin.getConfig().getStringList("gui.admin_requests_view.request_item.lore"));

        // Commands: tokens
        cmdTokensUsage = Util.translateColors(plugin.getConfig().getString("commands.tokens.usage", "&eUsage: /tokens give <player> <amount>"));
        cmdTokensAmountPositive = Util.translateColors(plugin.getConfig().getString("commands.tokens.amount_positive", "&cAmount must be a positive integer."));
        cmdTokensInvalidAmount = Util.translateColors(plugin.getConfig().getString("commands.tokens.invalid_amount", "&cInvalid amount specified."));
        cmdTokensUnknownSubcommand = Util.translateColors(plugin.getConfig().getString("commands.tokens.unknown_subcommand", "&cUnknown sub-command. Usage: /tokens give <player> <amount>"));

        // Title Properties
        titleMinLength = plugin.getConfig().getInt("title_properties.min_length", 3);
        titleMaxLength = plugin.getConfig().getInt("title_properties.max_length", 30);
        titleStatusApprovedText = Util.translateColors(plugin.getConfig().getString("title_properties.status_approved_text", "&aApproved"));
        titleStatusRejectedText = Util.translateColors(plugin.getConfig().getString("title_properties.status_rejected_text", "&cRejected"));
        titleStatusPendingText = Util.translateColors(plugin.getConfig().getString("title_properties.status_pending_text", "&ePending Review"));
        titleRequestCost = plugin.getConfig().getInt("title_properties.title_request_cost", 1);
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
    

    @SuppressWarnings("unchecked")
    public void loadPendingRequests() {
        pendingRequestsList.clear();
        if (pendingTitlesConfig.contains("requests")) {
            List<Map<?, ?>> requestsData = pendingTitlesConfig.getMapList("requests");
            for (Map<?, ?> reqData : requestsData) {
                UUID playerUUID = UUID.fromString((String) reqData.get("uuid"));
                String playerName = (String) reqData.get("playerName");
                String title = (String) reqData.get("title");
                long timestamp = reqData.containsKey("submissionTimestamp") ? (long) reqData.get("submissionTimestamp") : System.currentTimeMillis(); // Default if missing
                pendingRequestsList.add(new TitleRequest(playerUUID, playerName, title, timestamp));
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
            reqMap.put("submissionTimestamp", req.getSubmissionTimestamp()); // New
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

        if (playerTitlesConfig.contains("players")) {
            for (String uuidString : playerTitlesConfig.getConfigurationSection("players").getKeys(false)) {
                UUID playerUUID = UUID.fromString(uuidString);
                String playerPath = "players." + uuidString;
                String playerName = playerTitlesConfig.getString(playerPath + ".playerName", "Unknown"); // Default if needed
                
                // Load tokens for the player
                int tokens = playerTitlesConfig.getInt(playerPath + ".tokens", 0);
                playerTokensMap.put(playerUUID, tokens); // Use the existing playerTokensMap

                List<PlayerTitle> titlesForPlayer = new ArrayList<>();
                if (playerTitlesConfig.isList(playerPath + ".titles")) {
                    List<Map<?, ?>> titlesData = playerTitlesConfig.getMapList(playerPath + ".titles");
                    for (Map<?, ?> titleData : titlesData) {
                        String title = (String) titleData.get("title");
                        String status = (String) titleData.get("status");
                        String approvalDate = (String) titleData.get("approvalDate");
                        Object adminApproverNameObj = titleData.get("adminApproverName");
                        String adminApproverName = (adminApproverNameObj != null) ? (String) adminApproverNameObj : "";
                        titlesForPlayer.add(new PlayerTitle(playerUUID, playerName, title, status, approvalDate, adminApproverName));
                    }
                }
                playerTitlesMap.put(playerUUID, titlesForPlayer);
            }
        }
    }

    public void addPlayerTitle(PlayerTitle playerTitle) {
        List<PlayerTitle> titles = playerTitlesMap.getOrDefault(playerTitle.getPlayerUUID(), new ArrayList<>());
        titles.add(playerTitle);
        playerTitlesMap.put(playerTitle.getPlayerUUID(), titles);
        savePlayerTitles();
    }
    
    public void removePlayerTitle(UUID playerUUID, String titleName) {
        List<PlayerTitle> titles = playerTitlesMap.get(playerUUID);
        if (titles != null) {
            titles.removeIf(title -> title.getTitle().equals(titleName));
            // Optional: if all titles removed, could remove player entry from map, or leave with empty list
            // if (titles.isEmpty()) {
            //     playerTitlesMap.remove(playerUUID); 
            // }
        }
        savePlayerTitles(); // Save changes
    }

    public List<PlayerTitle> getPlayerTitles(UUID playerUUID) {
        return playerTitlesMap.getOrDefault(playerUUID, new ArrayList<>()); // Return empty list, not null
    }
    
    public void removeAllPlayerTitles(UUID playerUUID) {
        playerTitlesMap.remove(playerUUID);
        // Tokens are kept, as per original logic.
        // If tokens should also be removed, uncomment:
        // playerTokensMap.remove(playerUUID);
        savePlayerTitles();
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
        playerTitlesConfig.set("players." + playerUUID.toString() + ".tokens", newAmount); // Path updated
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
        SimpleDateFormat formatter = new SimpleDateFormat(this.timeFormat);
        return formatter.format(new Date());
    }

    // Rewritten savePlayerTitles
    public void savePlayerTitles() { // Removed @Override
        // Clear the existing "players" section before saving to avoid orphaned data
        playerTitlesConfig.set("players", null); 

        for (Map.Entry<UUID, List<PlayerTitle>> entry : playerTitlesMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            List<PlayerTitle> titlesList = entry.getValue();
            String playerPath = "players." + playerUUID.toString();

            if (titlesList.isEmpty() && !playerTokensMap.containsKey(playerUUID)) { 
                if (!playerTokensMap.containsKey(playerUUID) || getPlayerTokens(playerUUID) == 0) continue;
            }
            
            String playerName = titlesList.isEmpty() ? "UnknownPlayer" : titlesList.get(0).getPlayerName(); 

            playerTitlesConfig.set(playerPath + ".playerName", playerName); 
            playerTitlesConfig.set(playerPath + ".tokens", getPlayerTokens(playerUUID)); // Save tokens

            List<Map<String, Object>> titlesData = new ArrayList<>();
            for (PlayerTitle pt : titlesList) {
                Map<String, Object> titleMap = new HashMap<>();
                titleMap.put("title", pt.getTitle());
                titleMap.put("status", pt.getStatus());
                titleMap.put("approvalDate", pt.getApprovalDate());
                titleMap.put("adminApproverName", pt.getAdminApproverName());
                titlesData.add(titleMap);
            }
            playerTitlesConfig.set(playerPath + ".titles", titlesData);
        }

        try {
            playerTitlesConfig.save(playerTitlesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player titles: " + e.getMessage());
        }
    }
    
    // Getter methods for configuration values
    public String getTimeFormat() {
        return timeFormat;
    }

    public int getLuckpermsSuffixPriority() {
        return luckpermsSuffixPriority;
    }

    public String getMsgTitleNotApproved() {
        return msgTitleNotApproved;
    }

    public String getMsgInsufficientTokens() {
        return msgInsufficientTokens;
    }

    public String getMsgTokensReceived() {
        return msgTokensReceived;
    }

    public String getMsgTokensSent() {
        return msgTokensSent;
    }

    public String getMsgPlayerNotFound() {
        return msgPlayerNotFound;
    }

    public String getMsgRequestSubmitted() {
        return msgRequestSubmitted;
    }

    public String getMsgTitleApproved() {
        return msgTitleApproved;
    }

    public String getMsgTitleRejected() {
        return msgTitleRejected;
    }

    // Add import java.util.Date;
    // Add import java.text.SimpleDateFormat;
    public String formatTimestamp(long timestamp) {
        // Assumes 'this.timeFormat' is loaded from config.yml (e.g., "dd.MM.yyyy HH:mm")
        SimpleDateFormat formatter = new SimpleDateFormat(this.timeFormat);
        return formatter.format(new Date(timestamp));
    }

    // Helper method to translate color codes in a list of strings
    // This method is now removed as its functionality is superseded by Util.translateStringList
    /*
    private List<String> translateStringList(List<String> list) {
        if (list == null || list.isEmpty()) { 
            return new ArrayList<>();
        }
        List<String> translatedList = new ArrayList<>();
        for (String s : list) {
            // This would be an old call if kept:
            // translatedList.add(ChatColor.translateAlternateColorCodes('&', s)); 
            // Should be using Util.translateColors(s) if this method was to be kept and updated,
            // but Util.translateStringList handles the loop already.
        }
        return translatedList;
    }
    */

    // Getter methods for all new fields will be added below this line in the next step.
    // For brevity, not listing all getters here again, but they will be implemented.

    // Getters for General Messages
    public String getMsgPlayerOnlyCommand() { return msgPlayerOnlyCommand; }
    public String getMsgPermissionDenied() { return msgPermissionDenied; }
    public String getMsgTitleInputPrompt() { return msgTitleInputPrompt; }
    public String getMsgTitleInputTooShort() { return msgTitleInputTooShort; }
    public String getMsgTitleInputTooLong() { return msgTitleInputTooLong; }
    public String getMsgTitleInputCancelled() { return msgTitleInputCancelled; }
    public String getMsgTitleEquipped() { return msgTitleEquipped; }
    public String getMsgErrorGeneric() { return msgErrorGeneric; }
    public String getMsgAdminRequestStale() { return msgAdminRequestStale; }
    public String getMsgAdminTitleApprovedFeedback() { return msgAdminTitleApprovedFeedback; }
    public String getMsgAdminTitleRejectedFeedback() { return msgAdminTitleRejectedFeedback; }

    // Getters for GUI: main_menu
    public String getGuiMainMenuTitle() { return guiMainMenuTitle; }
    public String getGuiMainMenuItemRequestName() { return guiMainMenuItemRequestName; }
    public List<String> getGuiMainMenuItemRequestLore() { return guiMainMenuItemRequestLore; }
    public String getGuiMainMenuItemRequestNoTokensLore() { return guiMainMenuItemRequestNoTokensLore; }
    public String getGuiMainMenuItemViewOwnedName() { return guiMainMenuItemViewOwnedName; }
    public List<String> getGuiMainMenuItemViewOwnedLore() { return guiMainMenuItemViewOwnedLore; }
    public Map<Integer, ItemStack> getGuiMainMenuDecorations() { return guiMainMenuDecorations; } // New getter

    // Getters for GUI: player_titles_view
    public String getGuiPlayerTitlesViewTitle() { return guiPlayerTitlesViewTitle; }
    public String getGuiPlayerTitlesViewItemNamePrefix() { return guiPlayerTitlesViewItemNamePrefix; }
    public List<String> getGuiPlayerTitlesViewItemLoreBase() { return guiPlayerTitlesViewItemLoreBase; }
    public List<String> getGuiPlayerTitlesViewItemLoreStatusLine() { return guiPlayerTitlesViewItemLoreStatusLine; }
    public List<String> getGuiPlayerTitlesViewItemLoreAdminLine() { return guiPlayerTitlesViewItemLoreAdminLine; }
    public List<String> getGuiPlayerTitlesViewItemLoreDateLine() { return guiPlayerTitlesViewItemLoreDateLine; }
    public List<String> getGuiPlayerTitlesViewItemLoreEquipInstruction() { return guiPlayerTitlesViewItemLoreEquipInstruction; }
    public List<String> getGuiPlayerTitlesViewItemLoreNotApprovedInstruction() { return guiPlayerTitlesViewItemLoreNotApprovedInstruction; }

    // Getters for GUI: admin_requests_view
    public String getGuiAdminRequestsViewTitle() { return guiAdminRequestsViewTitle; }
    public String getGuiAdminRequestsViewNoRequestsItemName() { return guiAdminRequestsViewNoRequestsItemName; }
    public List<String> getGuiAdminRequestsViewNoRequestsItemLore() { return guiAdminRequestsViewNoRequestsItemLore; }
    public String getGuiAdminRequestsViewRequestItemNamePrefix() { return guiAdminRequestsViewRequestItemNamePrefix; }
    public List<String> getGuiAdminRequestsViewRequestItemLore() { return guiAdminRequestsViewRequestItemLore; }
    
    // Getters for Commands: tokens
    public String getCmdTokensUsage() { return cmdTokensUsage; }
    public String getCmdTokensAmountPositive() { return cmdTokensAmountPositive; }
    public String getCmdTokensInvalidAmount() { return cmdTokensInvalidAmount; }
    public String getCmdTokensUnknownSubcommand() { return cmdTokensUnknownSubcommand; }

    // Getters for Title Properties
    public int getTitleMinLength() { return titleMinLength; }
    public int getTitleMaxLength() { return titleMaxLength; }
    public String getTitleStatusApprovedText() { return titleStatusApprovedText; }
    public String getTitleStatusRejectedText() { return titleStatusRejectedText; }
    public String getTitleStatusPendingText() { return titleStatusPendingText; }
    public int getTitleRequestCost() { return titleRequestCost; }
}
