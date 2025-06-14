package ru.templetitles;

import org.bukkit.plugin.java.JavaPlugin;
import ru.templetitles.DataManager;
import ru.templetitles.CustomTitulCommand;
import ru.templetitles.CustomTitulsAdminCommand;
import ru.templetitles.GUIListener;
import ru.templetitles.ChatListener;
import ru.templetitles.TitleInputManager;
import org.bukkit.Bukkit;
import ru.templetitles.TimeUtil; // Added for TimeUtil.init()

public final class TempleTitles extends JavaPlugin {

    private DataManager dataManager;
    private TitleInputManager titleInputManager;

    @Override
    public void onEnable() {
        getLogger().info("Enabling TempleTitles...");

        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();
            if (created) {
                getLogger().info("Plugin data folder created.");
            }
        }
        
        saveDefaultConfig(); // Copies config.yml if not present
        dataManager = new DataManager(this); // DataManager loads the config
        TimeUtil.init(dataManager);          // Initialize TimeUtil with loaded DataManager
        titleInputManager = new TitleInputManager();

        // Register commands
        this.getCommand("customtitul").setExecutor(new CustomTitulCommand(this));
        this.getCommand("customtituls").setExecutor(new CustomTitulsAdminCommand(this));
        TokenCommand tokenCommand = new TokenCommand(this, dataManager);
        this.getCommand("tokens").setExecutor(tokenCommand);
        this.getCommand("titleban").setExecutor(new TitleBanCommand(this, dataManager));
        this.getCommand("titleunban").setExecutor(new TitleUnbanCommand(this, dataManager)); // New
        getLogger().info("Commands registered.");

        // Register event listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this, dataManager, titleInputManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, dataManager, titleInputManager), this);
        getLogger().info("Event listeners registered.");

        // TODO: Implement Token System integration here
        // - Check player token balance before allowing title creation
        // - Deduct tokens upon successful title request or approval

        // Schedule auto-saving task
        int intervalMinutes = dataManager.getAutoSaveIntervalMinutes();
        if (intervalMinutes > 0) {
            long intervalTicks = 20L * 60 * intervalMinutes; // Convert minutes to ticks
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> dataManager.saveAllData(false),
                intervalTicks,  // Initial delay
                intervalTicks); // Period
            getLogger().info("Data auto-saving scheduled every " + intervalMinutes + " minutes.");
        } else {
            getLogger().info("Data auto-saving is disabled.");
        }

        getLogger().info("TempleTitles plugin has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling TempleTitles plugin...");
        if (dataManager != null) {
            getLogger().info("Saving all plugin data...");
            dataManager.saveAllData(true); // Force save all dirty data
            getLogger().info("Plugin data saved.");
        }
        // Cancel all tasks scheduled by this plugin to prevent errors on reload
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("TempleTitles plugin has been disabled.");
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }

    public TitleInputManager getTitleInputManager() {
        return titleInputManager;
    }
}
