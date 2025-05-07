package fr.ju.privateMines;
import org.bukkit.plugin.java.JavaPlugin;

import fr.ju.privateMines.api.PrivateMinesAPI;
import fr.ju.privateMines.commands.MineCommand;
import fr.ju.privateMines.commands.MineTabCompleter;
import fr.ju.privateMines.listeners.GUIListener;
import fr.ju.privateMines.listeners.MineListener;
import fr.ju.privateMines.listeners.MineStatsListener;
import fr.ju.privateMines.managers.HologramManager;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.managers.MineWorldManager;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.placeholders.PrivateMinesPlaceholders;
import fr.ju.privateMines.utils.CacheManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.ConfigValidator;
import fr.ju.privateMines.utils.DependencyManager;
import fr.ju.privateMines.utils.ErrorHandler;
import fr.ju.privateMines.utils.GUIManager;
public class PrivateMines extends JavaPlugin {
    private static PrivateMines instance;
    private ConfigManager configManager;
    private MineManager mineManager;
    private MineWorldManager mineWorldManager;
    private StatsManager statsManager;
    private HologramManager hologramManager;
    private PrivateMinesAPI api;
    private DependencyManager dependencyManager;
    private ErrorHandler errorHandler;
    private CacheManager cacheManager;
    private GUIManager guiManager;
    private static boolean debugMode = true;
    @Override
    public void onEnable() {
        instance = this;
        errorHandler = new ErrorHandler(this);
        dependencyManager = new DependencyManager();
        cacheManager = new CacheManager();
        try {
            if (!checkAndHandleDependencies()) return;
            if (!loadAndValidateConfig()) return;
            initializeManagers();
            initializeHolograms();
            initializeAPI();
            registerCommandsAndListeners();
            registerPlaceholders();
            scheduleHologramUpdates();
            logPluginInfo();
        } catch (Exception e) {
            errorHandler.logError("Error while activating the plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    private boolean checkAndHandleDependencies() {
        dependencyManager.checkDependencies();
        if (!dependencyManager.areRequiredDependenciesPresent()) {
            errorHandler.logError("Missing dependencies! The plugin cannot start.", null);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }
    private boolean loadAndValidateConfig() {
        configManager = new ConfigManager(this);
        ConfigValidator configValidator = new ConfigValidator(this, configManager.getConfig());
        if (!configValidator.validateConfig()) {
            configValidator.logErrors();
            errorHandler.logError("Invalid configuration! The plugin cannot start.", null);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }
    private void initializeManagers() {
        mineWorldManager = new MineWorldManager(this);
        mineManager = new MineManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GUIManager(this);
    }
    private void initializeHolograms() {
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            hologramManager = new HologramManager(this);
            errorHandler.logInfo("DecentHolograms detected. Hologram support enabled!");
        } else {
            errorHandler.logWarning("DecentHolograms not found. Hologram support disabled.");
        }
    }
    private void initializeAPI() {
        PrivateMinesAPI.init(this);
        api = PrivateMinesAPI.getInstance();
    }
    private void registerCommandsAndListeners() {
        getCommand("jumine").setExecutor(new MineCommand(this));
        getCommand("jumine").setTabCompleter(new MineTabCompleter(this));
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new MineStatsListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    private void registerPlaceholders() {
        if (dependencyManager.isDependencyPresent("PlaceholderAPI")) {
            new PrivateMinesPlaceholders(this).register();
        }
    }
    private void scheduleHologramUpdates() {
        if (hologramManager != null) {
            getServer().getScheduler().runTaskLater(this, () -> hologramManager.updateAllHolograms(), 40L);
            getServer().getScheduler().runTaskTimer(this, () -> hologramManager.updateAllHolograms(), 20L * 60 * 5, 20L * 60 * 5);
        }
    }
    private void logPluginInfo() {
        errorHandler.logInfo("PrivateMines plugin successfully activated!");
        errorHandler.logInfo("Version: " + getPluginMeta().getVersion());
        errorHandler.logInfo("WorldGuard: " + dependencyManager.getDependencyVersion("WorldGuard"));
        errorHandler.logInfo("WorldEdit: " + dependencyManager.getDependencyVersion("WorldEdit"));
        if (dependencyManager.isDependencyPresent("PlaceholderAPI")) {
            errorHandler.logInfo("PlaceholderAPI: " + dependencyManager.getDependencyVersion("PlaceholderAPI"));
        }
    }
    @Override
    public void onDisable() {
        try {
            if (statsManager != null) {
                statsManager.saveStats();
            }
            if (mineManager != null) {
                mineManager.saveAllMineData();
            }
            errorHandler.logInfo("PrivateMines plugin successfully deactivated!");
        } catch (Exception e) {
            errorHandler.logError("Error while deactivating the plugin", e);
        }
    }
    public static PrivateMines getInstance() {
        return instance;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public MineManager getMineManager() {
        return mineManager;
    }
    public MineWorldManager getMineWorldManager() {
        return mineWorldManager;
    }
    public StatsManager getStatsManager() {
        return statsManager;
    }
    public HologramManager getHologramManager() {
        return hologramManager;
    }
    public PrivateMinesAPI getAPI() {
        return api;
    }
    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    public GUIManager getGUIManager() {
        return guiManager;
    }
    public boolean reloadPlugin() {
        errorHandler.logInfo("Starting to reload PrivateMines plugin...");
        try {
            savePluginData();
            unregisterListenersAndTasks();
            if (!reloadConfiguration()) {
                return false;
            }
            resetAndRecreateManagers();
            reloadPlaceholdersAndProtections();
            errorHandler.logInfo("Reload completed successfully!");
            return true;
        } catch (Exception e) {
            errorHandler.logError("Error during reload", e);
            errorHandler.logError("The plugin may not function correctly. Please restart the server.", null);
            return false;
        }
    }
    private void savePluginData() {
        errorHandler.logInfo("Saving data in progress...");
        if (statsManager != null) {
            statsManager.saveStats();
        }
        if (mineManager != null) {
            mineManager.saveAllMineData();
        }
        cacheManager.clear();
    }
    private void unregisterListenersAndTasks() {
        // Unregister all listeners
        org.bukkit.event.HandlerList.unregisterAll(this);
        errorHandler.logInfo("All listeners unregistered.");
        // Cancel all plugin tasks
        getServer().getScheduler().cancelTasks(this);
        errorHandler.logInfo("All plugin tasks cancelled.");
    }
    private boolean reloadConfiguration() {
        errorHandler.logInfo("Reloading configurations...");
        configManager.reloadConfig();
        configManager.reloadData();
        ConfigValidator configValidator = new ConfigValidator(this, configManager.getConfig());
        if (!configValidator.validateConfig()) {
            configValidator.logErrors();
            errorHandler.logError("Invalid configuration after reload!", null);
            return false;
        }
        return true;
    }
    private void resetAndRecreateManagers() {
        errorHandler.logInfo("Resetting managers...");
        this.mineWorldManager = null;
        this.mineManager = null;
        this.statsManager = null;
        this.hologramManager = null;
        this.guiManager = null;
        System.gc(); // Forcer le GC pour libérer les anciennes instances
        
        this.mineWorldManager = new MineWorldManager(this);
        this.mineManager = new MineManager(this);
        this.statsManager = new StatsManager(this);
        this.guiManager = new GUIManager(this);
        
        initializeHologramsAfterReload();
        
        errorHandler.logInfo("Reloading mine tiers...");
        mineManager.loadMineTiers();
        
        reregisterCommands();
        reregisterListeners();
    }
    private void initializeHologramsAfterReload() {
        if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            this.hologramManager = new HologramManager(this);
            errorHandler.logInfo("DecentHolograms reloaded. Hologram support enabled!");
            getServer().getScheduler().runTaskLater(this, () -> hologramManager.updateAllHolograms(), 20L);
        } else {
            this.hologramManager = null;
            errorHandler.logWarning("DecentHolograms not found. Hologram support disabled.");
        }
    }
    private void reregisterCommands() {
        errorHandler.logInfo("Reloading commands...");
        getCommand("jumine").setExecutor(new MineCommand(this));
        getCommand("jumine").setTabCompleter(new MineTabCompleter(this));
    }
    private void reregisterListeners() {
        errorHandler.logInfo("Registering new listeners...");
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new MineStatsListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    private void reloadPlaceholdersAndProtections() {
        if (dependencyManager.isDependencyPresent("PlaceholderAPI")) {
            errorHandler.logInfo("Reloading placeholders...");
            new PrivateMinesPlaceholders(this).register();
        }
        
        if (dependencyManager.isDependencyPresent("WorldGuard")) {
            errorHandler.logInfo("Restoring WorldGuard protections...");
            for (fr.ju.privateMines.models.Mine mine : mineManager.getAllMines()) {
                mineManager.getMineProtectionManager().updateMineProtection(mine);
            }
        } else {
            errorHandler.logWarning("WorldGuard is not present, protections will not be restored!");
        }
    }
    public static boolean isDebugMode() {
        return debugMode;
    }
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
    public static void debugLog(String message) {
        if (isDebugMode()) getInstance().getLogger().info(message);
    }
}
