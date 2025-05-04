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
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier stats.yml : " + e.getMessage());
                return;
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        setStatsConfig(statsConfig);
        ConfigurationSection minesSection = statsConfig.getConfigurationSection("mines");
        if (minesSection != null) {
            for (String key : minesSection.getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);
                    MineStats stats = new MineStats(owner);
                    ConfigurationSection mineSection = minesSection.getConfigurationSection(key);
                    if (mineSection != null) {
                        stats.setTotalBlocks(mineSection.getInt("total-blocks", 0));
                        if (mineSection.contains("blocks-mined")) {
                            int blocksMined = mineSection.getInt("blocks-mined", 0);
                            stats.setBlocksMined(blocksMined);
                            plugin.getLogger().info("[DEBUG] Loaded " + blocksMined + " mined blocks for UUID " + key);
                        }
                        if (mineSection.contains("last-reset")) {
                            long lastReset = mineSection.getLong("last-reset", System.currentTimeMillis());
                            stats.setLastReset(lastReset);
                        }
                    }
                    mineStats.put(owner, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID invalide dans stats.yml : " + key);
                }
            }
        }
        plugin.getLogger().info("Statistiques chargées pour " + mineStats.size() + " mines");
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
        if (!isEnabled()) return;
        Map<UUID, MineStats> mineStats = getMineStats();
        FileConfiguration statsConfig = getStatsConfig();
        File statsFile = getStatsFile();
        for (Map.Entry<UUID, MineStats> entry : mineStats.entrySet()) {
            UUID owner = entry.getKey();
            MineStats stats = entry.getValue();
            String path = "mines." + owner.toString();
            statsConfig.set(path + ".total-blocks", stats.getTotalBlocks());
            statsConfig.set(path + ".blocks-mined", stats.getBlocksMined());
            statsConfig.set(path + ".percentage-mined", stats.getPercentageMined());
            statsConfig.set(path + ".last-reset", stats.getLastReset());
        }
        try {
            statsConfig.save(statsFile);
            boolean notifyConsole = plugin.getConfigManager().getConfig().getBoolean("Statistics.notifications.console", false);
            boolean notifyInGame = plugin.getConfigManager().getConfig().getBoolean("Statistics.notifications.in-game", false);
            if (notifyConsole) {
                plugin.getLogger().info("Statistiques sauvegardées avec succès !");
            }
            if (notifyInGame) {
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.hasPermission("privateMines.admin")) {
                        player.sendMessage(fr.ju.privateMines.utils.ColorUtil.deserialize(
                            plugin.getConfigManager().getMessage("Messages.stats-saved")
                        ));
                    }
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