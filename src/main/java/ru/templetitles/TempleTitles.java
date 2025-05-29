package ru.templetitles;

import org.bukkit.plugin.java.JavaPlugin;
import ru.templetitles.DataManager;
import ru.templetitles.CustomTitulCommand;
import ru.templetitles.CustomTitulsAdminCommand;
import ru.templetitles.GUIListener;
import ru.templetitles.ChatListener;
import ru.templetitles.TitleInputManager;
// import org.bukkit.Bukkit; // Not directly used here but GUIListener uses it.

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
        
        dataManager = new DataManager(this);
        titleInputManager = new TitleInputManager();

        // Register commands
        this.getCommand("customtitul").setExecutor(new CustomTitulCommand(this));
        this.getCommand("customtituls").setExecutor(new CustomTitulsAdminCommand(this));
        getLogger().info("Commands registered.");

        // Register event listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this, dataManager, titleInputManager), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, dataManager, titleInputManager), this);
        getLogger().info("Event listeners registered.");

        // TODO: Implement Token System integration here
        // - Check player token balance before allowing title creation
        // - Deduct tokens upon successful title request or approval

        getLogger().info("TempleTitles plugin has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling TempleTitles plugin...");
        // Data is saved on modification by DataManager's methods upon add/remove actions.
        // Explicit saves here would be redundant unless there are specific operations
        // that don't trigger saves in DataManager.
        if (dataManager != null) {
            // Example: dataManager.saveAllDataIfNecessary(); // If such a method existed
            getLogger().info("Data saving is handled by DataManager during operations.");
        }
        getLogger().info("TempleTitles plugin has been disabled.");
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }

    public TitleInputManager getTitleInputManager() {
        return titleInputManager;
    }
}
