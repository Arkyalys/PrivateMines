package fr.ju.privateMines.services;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.MineStats;
public class StatsPersistenceService {
    private final PrivateMines plugin;
    private final StatsManager statsManager;
    public StatsPersistenceService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }
    public void loadStats() {
        File statsFile = getStatsFile();
        Map<UUID, MineStats> mineStats = getMineStats();
        FileConfiguration statsConfig = getStatsConfig();
        
        if (!ensureStatsFileExists(statsFile)) {
            return;
        }
        
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        setStatsConfig(statsConfig);
        
        ConfigurationSection minesSection = statsConfig.getConfigurationSection("mines");
        if (minesSection != null) {
            loadMinesFromConfig(minesSection, mineStats);
        }
        
        plugin.getLogger().info("Statistiques chargées pour " + mineStats.size() + " mines");
    }
    
    /**
     * S'assure que le fichier de statistiques existe, le crée si nécessaire.
     * @return true si le fichier existe ou a été créé avec succès, false en cas d'erreur
     */
    private boolean ensureStatsFileExists(File statsFile) {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier stats.yml : " + e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Charge les statistiques de toutes les mines à partir de la section de configuration.
     */
    private void loadMinesFromConfig(ConfigurationSection minesSection, Map<UUID, MineStats> mineStats) {
        for (String key : minesSection.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                ConfigurationSection mineSection = minesSection.getConfigurationSection(key);
                
                if (mineSection != null) {
                    MineStats stats = loadMineStats(owner, mineSection);
                    mineStats.put(owner, stats);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID invalide dans stats.yml : " + key);
            }
        }
    }
    
    /**
     * Charge les statistiques d'une mine spécifique.
     */
    private MineStats loadMineStats(UUID owner, ConfigurationSection mineSection) {
        MineStats stats = new MineStats(owner);
        
        stats.setTotalBlocks(mineSection.getInt("total-blocks", 0));
        
        if (mineSection.contains("blocks-mined")) {
            int blocksMined = mineSection.getInt("blocks-mined", 0);
            stats.setBlocksMined(blocksMined);
            logDebugInfo(owner, blocksMined);
        }
        
        if (mineSection.contains("last-reset")) {
            long lastReset = mineSection.getLong("last-reset", System.currentTimeMillis());
            stats.setLastReset(lastReset);
        }
        
        return stats;
    }
    
    /**
     * Journalise les informations de débogage sur les blocs minés.
     */
    private void logDebugInfo(UUID owner, int blocksMined) {
        plugin.getLogger().info("[DEBUG] Loaded " + blocksMined + " mined blocks for UUID " + owner);
    }
    public void startSaveTask() {
        int saveInterval = getSaveInterval();
        int ticks = saveInterval * 60 * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                saveStats();
            }
        }.runTaskTimerAsynchronously(plugin, ticks, ticks);
    }
    public void saveStats() {
        if (!isEnabled()) {
            return;
        }

        // If the plugin is already disabled (for example during onDisable), we
        // cannot schedule new asynchronous tasks. In that case, perform the save
        // synchronously in the current thread.
        if (!plugin.isEnabled()) {
            performSave();
            return;
        }

        // Otherwise ensure the save is executed asynchronously if we are on the
        // main server thread.
        if (plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::performSave);
        } else {
            performSave();
        }
    }
    
    /**
     * Effectue la sauvegarde des statistiques dans un contexte asynchrone.
     * Cette méthode ne doit pas être appelée directement depuis le thread principal.
     */
    private void performSave() {
        Map<UUID, MineStats> mineStats = getMineStats();
        FileConfiguration statsConfig = getStatsConfig();
        File statsFile = getStatsFile();
        
        // Créer une configuration entièrement nouvelle pour éviter des problèmes de concurrence
        FileConfiguration newConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, MineStats> entry : mineStats.entrySet()) {
            UUID owner = entry.getKey();
            MineStats stats = entry.getValue();
            String path = "mines." + owner.toString();
            newConfig.set(path + ".total-blocks", stats.getTotalBlocks());
            newConfig.set(path + ".blocks-mined", stats.getBlocksMined());
            newConfig.set(path + ".percentage-mined", stats.getPercentageMined());
            newConfig.set(path + ".last-reset", stats.getLastReset());
        }
        
        try {
            newConfig.save(statsFile);
            setStatsConfig(newConfig);
            
            // Notifications
            boolean notifyConsole = plugin.getConfigManager().getConfig().getBoolean("Config.Statistics.notifications.console", false);
            boolean notifyInGame = plugin.getConfigManager().getConfig().getBoolean("Config.Statistics.notifications.in-game", false);
            
            if (notifyConsole) {
                plugin.getLogger().info("Statistiques sauvegardées avec succès !");
            }
            
            if (notifyInGame) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().forEach(player -> {
                        if (player.hasPermission("privateMines.admin")) {
                            player.sendMessage(fr.ju.privateMines.utils.ColorUtil.deserialize(
                                plugin.getConfigManager().getMessage("Messages.stats-saved")
                            ));
                        }
                    });
                });
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }
    private File getStatsFile() { return statsManager.statsFile; }
    private Map<UUID, MineStats> getMineStats() { return statsManager.mineStats; }
    private FileConfiguration getStatsConfig() { return statsManager.statsConfig; }
    private void setStatsConfig(FileConfiguration config) { statsManager.statsConfig = config; }
    private int getSaveInterval() { return statsManager.saveInterval; }
    private boolean isEnabled() { return statsManager.enabled; }
} 