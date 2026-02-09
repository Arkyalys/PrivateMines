package fr.ju.privateMines.services;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
public class StatsSyncService {
    private final PrivateMines plugin;
    private final StatsManager statsManager;
    public StatsSyncService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }
    public void syncMineStats(Mine mine) {
        if (!statsManager.isEnabled()) return;
        UUID ownerUUID = mine.getOwner();
        MineStats statsManagerStats = statsManager.getMineStats().computeIfAbsent(ownerUUID, uuid -> new MineStats(uuid));
        MineStats mineStats = mine.getStats();
        int totalBlocks = mineStats.getTotalBlocks();
        if (totalBlocks > 0) {
            statsManagerStats.setTotalBlocks(totalBlocks);
            PrivateMines.debugLog("[DEBUG] Synchronized totalBlocks: " + totalBlocks + " for UUID " + ownerUUID);
        } else if (mine.hasMineArea()) {
            int calculatedTotal = (int) Math.min(
                (long)(mine.getMaxX() - mine.getMinX() + 1) *
                (mine.getMaxY() - mine.getMinY() + 1) *
                (mine.getMaxZ() - mine.getMinZ() + 1),
                Integer.MAX_VALUE);
            statsManagerStats.setTotalBlocks(calculatedTotal);
            mineStats.setTotalBlocks(calculatedTotal);
            PrivateMines.debugLog("[DEBUG] Recalculated and synchronized totalBlocks: " + calculatedTotal + " for UUID " + ownerUUID);
        }
        int blocksMined = statsManagerStats.getBlocksMined();
        if (blocksMined > 0) {
            if (mineStats.getBlocksMined() < blocksMined) {
                mineStats.setBlocksMined(blocksMined);
                PrivateMines.debugLog("[DEBUG] Updated mine's blocksMined to match StatsManager: " + blocksMined);
            } else if (mineStats.getBlocksMined() > blocksMined) {
                statsManagerStats.setBlocksMined(mineStats.getBlocksMined());
                PrivateMines.debugLog("[DEBUG] Updated StatsManager's blocksMined to match mine: " + mineStats.getBlocksMined());
            }
        }
        String path = "mines." + ownerUUID.toString();
        FileConfiguration statsConfig = statsManager.statsConfig;
        File statsFile = statsManager.statsFile;
        statsConfig.set(path + ".total-blocks", statsManagerStats.getTotalBlocks());
        statsConfig.set(path + ".blocks-mined", statsManagerStats.getBlocksMined());
        try {
            statsConfig.save(statsFile);
            PrivateMines.debugLog("[DEBUG] Saved updated stats to stats.yml for UUID " + ownerUUID);
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save stats.yml after synchronization: " + e.getMessage());
        }
    }
} 