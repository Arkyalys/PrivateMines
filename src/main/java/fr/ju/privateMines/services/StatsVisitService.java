package fr.ju.privateMines.services;
import java.util.UUID;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
public class StatsVisitService {
    private final PrivateMines plugin;
    private final StatsManager statsManager;
    public StatsVisitService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }
    public void addVisit(Mine mine, UUID visitor) {
        if (!statsManager.isEnabled()) {
            plugin.getLogger().warning("[DEBUG] StatsManager.addVisit: Stats system is disabled, visit will not be recorded");
            return;
        }
        plugin.getLogger().info("[DEBUG] StatsManager.addVisit: Processing visit for UUID " + visitor + " to mine owned by " + mine.getOwner());
        UUID ownerUUID = mine.getOwner();
        MineStats statsManagerStats = statsManager.getMineStats().get(ownerUUID);
        if (statsManagerStats == null) {
            plugin.getLogger().info("[DEBUG] StatsManager.addVisit: Creating new MineStats for owner " + ownerUUID);
            statsManagerStats = new MineStats(ownerUUID);
            statsManager.getMineStats().put(ownerUUID, statsManagerStats);
        }
        try {
            int beforeVisits = statsManagerStats.getVisits();
            statsManagerStats.addVisit(visitor);
            plugin.getLogger().info("[DEBUG] StatsManager.addVisit: Visit added in StatsManager. Visits before: " + beforeVisits + ", after: " + statsManagerStats.getVisits());
        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] StatsManager.addVisit: Error adding visit in StatsManager: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            MineStats mineStats = mine.getStats();
            if (mineStats != null) {
                int beforeVisits = mineStats.getVisits();
                mineStats.addVisit(visitor);
                plugin.getLogger().info("[DEBUG] StatsManager.addVisit: Visit added directly to Mine stats. Visits before: " + beforeVisits + ", after: " + mineStats.getVisits());
            } else {
                plugin.getLogger().warning("[DEBUG] StatsManager.addVisit: Mine stats object is null!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] StatsManager.addVisit: Error updating Mine stats: " + e.getMessage());
            e.printStackTrace();
        }
        if (Math.random() < 0.1) {
            plugin.getLogger().info("[DEBUG] StatsManager.addVisit: Random save triggered");
            statsManager.saveStats();
        }
    }
} 