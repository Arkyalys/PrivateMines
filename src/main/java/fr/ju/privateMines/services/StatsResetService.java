package fr.ju.privateMines.services;
import java.util.UUID;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
public class StatsResetService {
    private final PrivateMines plugin;
    private final StatsManager statsManager;
    public StatsResetService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }
    public void onMineReset(Mine mine) {
        if (!statsManager.isEnabled()) return;
        UUID ownerUUID = mine.getOwner();
        MineStats stats = statsManager.getMineStats().computeIfAbsent(ownerUUID, uuid -> new MineStats(uuid));
        int totalBlocks = stats.getTotalBlocks();
        if (totalBlocks <= 0 && mine.hasMineArea()) {
            totalBlocks = (int) Math.min(
                (long)(mine.getMaxX() - mine.getMinX() + 1) *
                (mine.getMaxY() - mine.getMinY() + 1) *
                (mine.getMaxZ() - mine.getMinZ() + 1),
                Integer.MAX_VALUE);
        }
        stats.resetBlockStats();
        stats.setTotalBlocks(totalBlocks);
        mine.getStats().setTotalBlocks(totalBlocks);
        statsManager.saveStats();
        PrivateMines.debugLog("[DEBUG] Reset stats for mine owned by " + ownerUUID +
                              ". totalBlocks = " + totalBlocks +
                              ", blocksMined = " + stats.getBlocksMined());
    }
} 