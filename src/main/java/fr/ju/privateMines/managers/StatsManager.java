package fr.ju.privateMines.managers;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.services.StatsBlocksService;
import fr.ju.privateMines.services.StatsPersistenceService;
import fr.ju.privateMines.services.StatsRankingService;
import fr.ju.privateMines.services.StatsResetService;
import fr.ju.privateMines.services.StatsSyncService;
import fr.ju.privateMines.services.StatsVisitService;
public class StatsManager {
    final PrivateMines plugin;
    public File statsFile;
    public FileConfiguration statsConfig;
    public final Map<UUID, MineStats> mineStats;
    public final boolean enabled;
    public final int saveInterval;
    public final int maxTrackedVisitors;
    private final StatsPersistenceService statsPersistenceService;
    private final StatsVisitService statsVisitService;
    private final StatsBlocksService statsBlocksService;
    private final StatsResetService statsResetService;
    private final StatsRankingService statsRankingService;
    private final StatsSyncService statsSyncService;
    public StatsManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.mineStats = new HashMap<>();
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("Statistics.enabled", true);
        this.saveInterval = plugin.getConfigManager().getConfig().getInt("Statistics.save-interval", 10);
        this.maxTrackedVisitors = plugin.getConfigManager().getConfig().getInt("Statistics.max-tracked-visitors", 50);
        this.statsPersistenceService = new StatsPersistenceService(plugin, this);
        this.statsVisitService = new StatsVisitService(plugin, this);
        this.statsBlocksService = new StatsBlocksService(plugin, this);
        this.statsResetService = new StatsResetService(plugin, this);
        this.statsRankingService = new StatsRankingService(plugin, this);
        this.statsSyncService = new StatsSyncService(plugin, this);
        if (enabled) {
            statsPersistenceService.loadStats();
            statsPersistenceService.startSaveTask();
        }
    }
    public MineStats getStats(UUID owner) {
        return mineStats.get(owner);
    }
    public boolean incrementBlocksMined(Mine mine) {
        return statsBlocksService.incrementBlocksMined(mine);
    }
    public void addVisit(Mine mine, UUID visitor) {
        statsVisitService.addVisit(mine, visitor);
    }
    public void onMineReset(Mine mine) {
        statsResetService.onMineReset(mine);
    }
    public void removeMineStats(UUID owner) {
        if (!enabled) return;
        mineStats.remove(owner);
        statsConfig.set("mines." + owner.toString(), null);
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder stats.yml : " + e.getMessage());
        }
    }
    public List<Mine> getTopMines() {
        return statsRankingService.getTopMines();
    }
    public void syncMineStats(Mine mine) {
        statsSyncService.syncMineStats(mine);
    }
    public void saveStats() {
        statsPersistenceService.saveStats();
    }
    public Map<UUID, MineStats> getMineStats() { return mineStats; }
    public boolean isEnabled() { return enabled; }
} 