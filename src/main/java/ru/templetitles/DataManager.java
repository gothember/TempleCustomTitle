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
import java.util.Arrays;
// java.io.File is already imported above
import org.bukkit.ChatColor;
import org.bukkit.Bukkit; // Added
import org.bukkit.OfflinePlayer; // Added
// For Util class:
// import ru.templetitles.Util;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.Set; // Added
import java.util.HashSet; // Added

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
    private Map<Integer, DecorationItemConfig> guiMainMenuDecorations; // Changed type

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
    private int titleRequestCost;
    private List<String> titleInputCancelKeywords;
    private boolean patternValidationEnabled;
    private List<Pattern> compiledForbiddenPatterns;
    private String msgTitlePatternViolation;
    private int maxPendingRequests;
    private String msgMaxPendingRequestsReached;
    // Ban messages
    private String msgTitleBanAppliedPlayer;
    private String msgTitleBanAppliedAdmin;
    private String msgTitleBanAttemptWhileBanned;
    private String msgPlayerNotBanned;
    private String msgTitleUnbanSuccessAdmin;
    private String msgTitleUnbanNotificationPlayer;
    private String msgInvalidTimeFormat;
    private String msgPlayerNeverPlayed; // For offline player handling in ban commands
    private String msgUsageTitleBan;
    private String msgUsageTitleUnban;
    // Ban data
    private Map<UUID, Long> titleCreationBans;

    // Time Unit Strings for TimeUtil.formatDuration
    private String timeUnitDaySingular;
    private String timeUnitDayPlural;
    private String timeUnitHourSingular;
    private String timeUnitHourPlural;
    private String timeUnitMinuteSingular;
    private String timeUnitMinutePlural;
    private String timeUnitSecondSingular;
    private String timeUnitSecondPlural;

    // Dirty flags for saving
    private boolean pendingDirty = false;
    private boolean titlesDirty = false;
    private int autoSaveIntervalMinutes;

    private File pendingTitlesFile;
    private FileConfiguration pendingTitlesConfig;
    private File playerTitlesFile;
    private FileConfiguration playerTitlesConfig;

    // Placeholder for actual data structures
    private List<TitleRequest> pendingRequestsList = new ArrayList<>();
    private Map<UUID, List<PlayerTitle>> playerTitlesMap = new HashMap<>(); // Changed
    private Map<UUID, Integer> playerTokensMap = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.titleCreationBans = new HashMap<>(); // Initialize ban map
        loadConfigFile();
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

        timeFormat = plugin.getConfig().getString("messages.time_format", "dd.MM.yyyy HH:mm"); // Not a colored message
        luckpermsSuffixPriority = plugin.getConfig().getInt("luckperms.suffix_priority", 1); // Not a colored message

        // Translate all user-facing messages, including older ones
        msgTitleNotApproved = Util.translateColors(plugin.getConfig().getString("messages.title_not_approved", "&cYour title '%title%' is not yet approved or has been rejected."));
        msgInsufficientTokens = Util.translateColors(plugin.getConfig().getString("messages.insufficient_tokens", "&cYou do not have enough tokens. Required: %required_tokens%."));
        msgTokensReceived = Util.translateColors(plugin.getConfig().getString("messages.tokens_received", "&aYou have received %amount% tokens."));
        msgTokensSent = Util.translateColors(plugin.getConfig().getString("messages.tokens_sent", "&aYou have sent %amount% tokens to %player%."));
        msgPlayerNotFound = Util.translateColors(plugin.getConfig().getString("messages.player_not_found", "&cPlayer %player% not found."));
        msgRequestSubmitted = Util.translateColors(plugin.getConfig().getString("messages.request_submitted", "&aYour title request for '%title%' has been submitted for review."));
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
                    List<String> commandList = decoSection.getStringList(slotKey + ".commands");
                    DecorationItemConfig decoConfig = new DecorationItemConfig(itemStack, commandList);
                    guiMainMenuDecorations.put(slot, decoConfig);
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

        // Load Title Input Cancel Keywords
        List<String> rawCancelKeywords = plugin.getConfig().getStringList("title_properties.cancel_keywords");
        if (rawCancelKeywords == null || rawCancelKeywords.isEmpty()) {
            rawCancelKeywords = new ArrayList<>(Arrays.asList("cancel", "отмена", "отменить", "quit", "exit"));
        }
        this.titleInputCancelKeywords = new ArrayList<>();
        for (String keyword : rawCancelKeywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.titleInputCancelKeywords.add(keyword.toLowerCase().trim());
            }
        }
        if (this.titleInputCancelKeywords.isEmpty()) { // Failsafe
            this.titleInputCancelKeywords.add("cancel");
        }

        // Load auto-save interval
        autoSaveIntervalMinutes = plugin.getConfig().getInt("saving.auto_save_interval_minutes", 5);

        // Load Title Pattern Validation Settings
        patternValidationEnabled = plugin.getConfig().getBoolean("title_properties.validation.enable_pattern_validation", true);
        msgTitlePatternViolation = Util.translateColors(plugin.getConfig().getString("title_properties.validation.pattern_match_warning_message", "&cYour title contains forbidden characters or patterns."));
        maxPendingRequests = plugin.getConfig().getInt("title_properties.max_pending_requests", 100); // New
        msgMaxPendingRequestsReached = Util.translateColors(plugin.getConfig().getString("messages.max_pending_requests_reached", "&cSorry, the title request queue is currently full. Please try again later.")); // New

        // Ban related messages from config
        msgTitleBanAppliedPlayer = Util.translateColors(plugin.getConfig().getString("messages.title_ban_applied_player", "&cАдминистратор %admin_name% запретил вам создавать титулы на %duration%."));
        msgTitleBanAppliedAdmin = Util.translateColors(plugin.getConfig().getString("messages.title_ban_applied_admin", "&aВы запретили игроку %player_name% создавать титулы на %duration%."));
        msgTitleBanAttemptWhileBanned = Util.translateColors(plugin.getConfig().getString("messages.title_ban_attempt_while_banned", "&cВы не можете создавать титулы. Блокировка истекает %expiry_date% (еще %remaining_time%)."));
        msgPlayerNotBanned = Util.translateColors(plugin.getConfig().getString("messages.player_not_banned", "&eИгрок %player_name% не имеет активной блокировки на создание титулов."));
        msgTitleUnbanSuccessAdmin = Util.translateColors(plugin.getConfig().getString("messages.title_unban_success_admin", "&aВы успешно разблокировали игрока %player_name% от создания титулов."));
        msgTitleUnbanNotificationPlayer = Util.translateColors(plugin.getConfig().getString("messages.title_unban_notification_player", "&aАдминистратор %admin_name% снял с вас блокировку на создание титулов."));
        msgInvalidTimeFormat = Util.translateColors(plugin.getConfig().getString("messages.invalid_time_format", "&cНеверный формат времени. Используйте, например: 30s, 10m, 1h, 7d."));
        msgPlayerNeverPlayed = Util.translateColors(plugin.getConfig().getString("messages.player_never_played", "&cИгрок %player_name% никогда не играл на сервере и не может быть заблокирован/разблокирован."));
        msgUsageTitleBan = Util.translateColors(plugin.getConfig().getString("messages.usage_titleban", "&eИспользование: /titleban <игрок> <время> (например, 1d, 2h, 30m)"));
        msgUsageTitleUnban = Util.translateColors(plugin.getConfig().getString("messages.usage_titleunban", "&eИспользование: /titleunban <игрок>"));

        // Time Unit Strings
        timeUnitDaySingular = Util.translateColors(plugin.getConfig().getString("time_format_units.day.singular", " day"));
        timeUnitDayPlural = Util.translateColors(plugin.getConfig().getString("time_format_units.day.plural", " days"));
        timeUnitHourSingular = Util.translateColors(plugin.getConfig().getString("time_format_units.hour.singular", " hour"));
        timeUnitHourPlural = Util.translateColors(plugin.getConfig().getString("time_format_units.hour.plural", " hours"));
        timeUnitMinuteSingular = Util.translateColors(plugin.getConfig().getString("time_format_units.minute.singular", " minute"));
        timeUnitMinutePlural = Util.translateColors(plugin.getConfig().getString("time_format_units.minute.plural", " minutes"));
        timeUnitSecondSingular = Util.translateColors(plugin.getConfig().getString("time_format_units.second.singular", " second"));
        timeUnitSecondPlural = Util.translateColors(plugin.getConfig().getString("time_format_units.second.plural", " seconds"));

        compiledForbiddenPatterns = new ArrayList<>();
        List<String> rawPatterns = plugin.getConfig().getStringList("title_properties.validation.forbidden_patterns");
        if (rawPatterns != null) {
            for (String patternStr : rawPatterns) {
                try {
                    if (patternStr != null && !patternStr.trim().isEmpty()) {
                        compiledForbiddenPatterns.add(Pattern.compile(patternStr));
                    }
                } catch (PatternSyntaxException e) {
                    plugin.getLogger().warning("Invalid regex pattern in config 'title_properties.validation.forbidden_patterns': " + patternStr + " - Error: " + e.getMessage());
                }
            }
        }
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

    private void executeSavePendingRequests() { // Renamed and made private
        try {
            pendingTitlesConfig.save(pendingTitlesFile);
            plugin.getLogger().info("Saved pending title requests to disk."); // Optional: log save
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pending titles: " + e.getMessage());
        }
    }

    // Renamed savePlayerTitles to executeSavePlayerTitles and made it private
    private void executeSavePlayerTitles() {
        playerTitlesConfig.set("players", null);
        for (Map.Entry<UUID, List<PlayerTitle>> entry : playerTitlesMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            List<PlayerTitle> titlesList = entry.getValue();
            String playerPath = "players." + playerUUID.toString();
            // Ensure player basic data (name, tokens) is saved even if titlesList is empty but tokens or ban exist
            boolean playerHasTokens = playerTokensMap.containsKey(playerUUID) && getPlayerTokens(playerUUID) > 0;
            // Check against current time for active ban
            boolean playerIsBanned = titleCreationBans.containsKey(playerUUID) && titleCreationBans.get(playerUUID) > System.currentTimeMillis();


            if (titlesList.isEmpty() && !playerHasTokens && !playerIsBanned) {
                continue; // Skip saving this player if they have no titles, no tokens, and no active ban
            }

            String playerName = "UnknownPlayer"; // Default player name
            if (!titlesList.isEmpty()) {
                playerName = titlesList.get(0).getPlayerName();
            } else { // If no titles, try to get name if player has tokens or is banned
                 OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                 if (offlinePlayer != null && offlinePlayer.getName() != null) {
                    playerName = offlinePlayer.getName();
                 }
            }

            playerTitlesConfig.set(playerPath + ".playerName", playerName);
            playerTitlesConfig.set(playerPath + ".tokens", getPlayerTokens(playerUUID)); // Saves 0 if not in map or 0

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

            // Save active ban status
            Long banExpiry = this.titleCreationBans.get(playerUUID);
            if (banExpiry != null && banExpiry > System.currentTimeMillis()) {
                playerTitlesConfig.set(playerPath + ".title_ban_expiry", banExpiry);
            } else {
                playerTitlesConfig.set(playerPath + ".title_ban_expiry", null); // Remove expired or non-existent ban
            }
        }

        // Save players who might only have tokens or bans (and no titles listed in playerTitlesMap)
        Set<UUID> allPlayerUUIDsWithData = new HashSet<>(playerTokensMap.keySet());
        allPlayerUUIDsWithData.addAll(titleCreationBans.keySet());

        for (UUID playerUUID : allPlayerUUIDsWithData) {
            String playerPath = "players." + playerUUID.toString();
            if (!playerTitlesConfig.contains(playerPath)) { // If not already saved by the playerTitlesMap loop
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                String playerName = (offlinePlayer != null && offlinePlayer.getName() != null) ? offlinePlayer.getName() : "UnknownPlayer";

                playerTitlesConfig.set(playerPath + ".playerName", playerName);
                playerTitlesConfig.set(playerPath + ".tokens", getPlayerTokens(playerUUID)); // Saves 0 if not in map
                playerTitlesConfig.set(playerPath + ".titles", new ArrayList<>()); // Empty titles list

                Long banExpiry = this.titleCreationBans.get(playerUUID);
                if (banExpiry != null && banExpiry > System.currentTimeMillis()) {
                    playerTitlesConfig.set(playerPath + ".title_ban_expiry", banExpiry);
                } else {
                    // Ensure it's not in config if not active or not present in map
                    playerTitlesConfig.set(playerPath + ".title_ban_expiry", null);
                }
            }
        }
        try {
            playerTitlesConfig.save(playerTitlesFile);
            plugin.getLogger().info("Saved player titles and tokens to disk."); // Optional: log save
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player titles: " + e.getMessage());
        }
    }

    public synchronized void saveAllData(boolean forceSave) {
        boolean savedSomething = false;
        if (forceSave || this.pendingDirty) {
            executeSavePendingRequests();
            this.pendingDirty = false;
            savedSomething = true;
        }
        if (forceSave || this.titlesDirty) {
            executeSavePlayerTitles();
            this.titlesDirty = false;
            savedSomething = true;
        }
        if (savedSomething && !forceSave && autoSaveIntervalMinutes > 0) { // Log only for auto-saves that did something
           plugin.getLogger().info("Auto-saved plugin data.");
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
            reqMap.put("submissionTimestamp", req.getSubmissionTimestamp());
            requestsData.add(reqMap);
        }
        pendingTitlesConfig.set("requests", requestsData);
        this.pendingDirty = true; // Mark as dirty
        // savePendingRequests(); // Removed
    }

    public void removePendingRequest(UUID playerUUID, String title) {
        boolean removed = pendingRequestsList.removeIf(req -> req.getPlayerUUID().equals(playerUUID) && req.getTitle().equals(title));
        if (removed) {
            List<Map<String, Object>> requestsData = new ArrayList<>();
            for (TitleRequest req : pendingRequestsList) {
                Map<String, Object> reqMap = new HashMap<>();
                reqMap.put("uuid", req.getPlayerUUID().toString());
                reqMap.put("playerName", req.getPlayerName());
                reqMap.put("title", req.getTitle());
                reqMap.put("submissionTimestamp", req.getSubmissionTimestamp()); // Ensure timestamp is saved
                requestsData.add(reqMap);
            }
            pendingTitlesConfig.set("requests", requestsData);
            this.pendingDirty = true; // Mark as dirty
            // savePendingRequests(); // Removed
        }
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

                // Load ban expiry
                if (playerTitlesConfig.contains(playerPath + ".title_ban_expiry")) {
                    long banExpiry = playerTitlesConfig.getLong(playerPath + ".title_ban_expiry");
                    if (banExpiry > System.currentTimeMillis()) {
                        this.titleCreationBans.put(playerUUID, banExpiry);
                    } else if (banExpiry > 0) {
                        // Optional: Clean up expired ban from config immediately
                        // playerTitlesConfig.set(playerPath + ".title_ban_expiry", null);
                        // this.titlesDirty = true;
                    }
                }
            }
        }
    }

    public void addPlayerTitle(PlayerTitle playerTitle) {
        List<PlayerTitle> titles = playerTitlesMap.getOrDefault(playerTitle.getPlayerUUID(), new ArrayList<>());
        titles.add(playerTitle);
        playerTitlesMap.put(playerTitle.getPlayerUUID(), titles);
        this.titlesDirty = true; // Mark as dirty
        // savePlayerTitles(); // Removed
    }
    
    public void removePlayerTitle(UUID playerUUID, String titleName) {
        List<PlayerTitle> titles = playerTitlesMap.get(playerUUID);
        if (titles != null) {
            boolean removed = titles.removeIf(title -> title.getTitle().equals(titleName));
            if (removed) {
                this.titlesDirty = true; // Mark as dirty if something was actually removed
            }
            // if (titles.isEmpty()) { playerTitlesMap.remove(playerUUID); } // Optional
        }
        // savePlayerTitles(); // Removed
    }

    public List<PlayerTitle> getPlayerTitles(UUID playerUUID) {
        return playerTitlesMap.getOrDefault(playerUUID, new ArrayList<>());
    }

    public void removeAllPlayerTitles(UUID playerUUID) {
        if (playerTitlesMap.containsKey(playerUUID)) {
            playerTitlesMap.remove(playerUUID);
            this.titlesDirty = true; // Mark as dirty
        }
        // savePlayerTitles(); // Removed
    }

    // Token Management Methods
    public int getPlayerTokens(UUID playerUUID) {
        return playerTokensMap.getOrDefault(playerUUID, 0);
    }

    public void setPlayerTokens(UUID playerUUID, int amount) {
        int newAmount = Math.max(0, amount); // Ensure tokens don't go negative
        playerTokensMap.put(playerUUID, newAmount);
        // Ensure the player's section exists if we are setting tokens.
        // This will be handled by the unified savePlayerTitles which saves both titles and tokens.
        this.titlesDirty = true; // Mark as dirty (as tokens are part of player_titles.yml)
        // savePlayerTitles(); // Removed
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
    // Getter methods for configuration values
    public int getAutoSaveIntervalMinutes() { return autoSaveIntervalMinutes; } // New getter
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

    // Inner class for Decoration Item Configuration
    public static class DecorationItemConfig {
        public final ItemStack itemStack;
        public final List<String> commands;

        public DecorationItemConfig(ItemStack itemStack, List<String> commands) {
            this.itemStack = itemStack;
            this.commands = (commands != null) ? commands : new ArrayList<>();
        }

        public ItemStack getItemStack() { return itemStack; }
        public List<String> getCommands() { return commands; }
    }

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
    public Map<Integer, DecorationItemConfig> getGuiMainMenuDecorations() { return guiMainMenuDecorations; } // Changed return type

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
    public List<String> getTitleInputCancelKeywords() { return titleInputCancelKeywords; }
    public boolean isPatternValidationEnabled() { return patternValidationEnabled; }
    public List<Pattern> getCompiledForbiddenPatterns() { return compiledForbiddenPatterns; }
    public String getMsgTitlePatternViolation() { return msgTitlePatternViolation; }
    public int getMaxPendingRequests() { return maxPendingRequests; }
    public String getMsgMaxPendingRequestsReached() { return msgMaxPendingRequestsReached; }

    // Getters for Ban Messages
    public String getMsgTitleBanAppliedPlayer() { return msgTitleBanAppliedPlayer; }
    public String getMsgTitleBanAppliedAdmin() { return msgTitleBanAppliedAdmin; }
    public String getMsgTitleBanAttemptWhileBanned() { return msgTitleBanAttemptWhileBanned; }
    public String getMsgPlayerNotBanned() { return msgPlayerNotBanned; }
    public String getMsgTitleUnbanSuccessAdmin() { return msgTitleUnbanSuccessAdmin; }
    public String getMsgTitleUnbanNotificationPlayer() { return msgTitleUnbanNotificationPlayer; }
    public String getMsgInvalidTimeFormat() { return msgInvalidTimeFormat; }
    public String getMsgPlayerNeverPlayed() { return msgPlayerNeverPlayed; }
    public String getMsgUsageTitleBan() { return msgUsageTitleBan; }
    public String getMsgUsageTitleUnban() { return msgUsageTitleUnban; }

    // Getters for Time Unit Strings
    public String getTimeUnitDaySingular() { return timeUnitDaySingular; }
    public String getTimeUnitDayPlural() { return timeUnitDayPlural; }
    public String getTimeUnitHourSingular() { return timeUnitHourSingular; }
    public String getTimeUnitHourPlural() { return timeUnitHourPlural; }
    public String getTimeUnitMinuteSingular() { return timeUnitMinuteSingular; }
    public String getTimeUnitMinutePlural() { return timeUnitMinutePlural; }
    public String getTimeUnitSecondSingular() { return timeUnitSecondSingular; }
    public String getTimeUnitSecondPlural() { return timeUnitSecondPlural; }

    // Ban Management Methods
    public void banPlayerTitleCreation(UUID playerUUID, long expiryTimestamp) {
        if (playerUUID == null) return;
        this.titleCreationBans.put(playerUUID, expiryTimestamp);
        this.titlesDirty = true;
    }

    public void unbanPlayerTitleCreation(UUID playerUUID) {
        if (playerUUID == null) return;
        if (this.titleCreationBans.remove(playerUUID) != null) {
            this.titlesDirty = true;
        }
    }

    public boolean isPlayerBannedFromTitleCreation(UUID playerUUID) {
        if (playerUUID == null) return false;
        Long expiryTimestamp = this.titleCreationBans.get(playerUUID);
        if (expiryTimestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() < expiryTimestamp) {
            return true;
        } else {
            this.titleCreationBans.remove(playerUUID); // Lazy cleanup
            this.titlesDirty = true;
            return false;
        }
    }

    public long getBanExpiryTimestamp(UUID playerUUID) {
        if (playerUUID == null) return 0L;
        Long expiry = this.titleCreationBans.get(playerUUID);
        return (expiry != null) ? expiry : 0L;
    }
}
