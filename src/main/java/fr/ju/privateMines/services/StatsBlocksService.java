package fr.ju.privateMines.services;
import java.util.UUID;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
public class StatsBlocksService {
    private final PrivateMines plugin;
    private final StatsManager statsManager;
    public StatsBlocksService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }
    public boolean incrementBlocksMined(Mine mine) {
        if (!statsManager.isEnabled()) return false;
        UUID ownerUUID = mine.getOwner();
        MineStats stats = statsManager.getMineStats().computeIfAbsent(ownerUUID, uuid -> new MineStats(uuid));
        if (stats.getTotalBlocks() <= 0 && mine.hasMineArea()) {
            int calculatedTotal = (mine.getMaxX() - mine.getMinX() + 1) *
                                 (mine.getMaxY() - mine.getMinY() + 1) *
                                 (mine.getMaxZ() - mine.getMinZ() + 1);
            stats.setTotalBlocks(calculatedTotal);
            mine.getStats().setTotalBlocks(calculatedTotal);
            plugin.getLogger().info("[DEBUG] Auto-fixed totalBlocks to " + calculatedTotal + " for UUID " + ownerUUID);
        }
        stats.incrementBlocksMined();
        plugin.getMetricsService().incrementBlocksMined();
        mine.getStats().setBlocksMined(stats.getBlocksMined());
        return stats.shouldAutoReset(
            plugin.getConfigManager().getConfig().getInt("Gameplay.auto-reset.threshold", 40)
        );
    }
} 