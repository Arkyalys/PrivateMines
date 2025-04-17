package fr.ju.privateMines.models;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class MineStats {
    private final UUID mineOwner;
    private int blocksMined;
    private int totalBlocks;
    private int visits;
    private long lastReset;
    private Map<UUID, Integer> visitorStats;
    public MineStats(UUID mineOwner) {
        this.mineOwner = mineOwner;
        this.blocksMined = 0;
        this.totalBlocks = 0;
        this.visits = 0;
        this.lastReset = System.currentTimeMillis();
        this.visitorStats = new HashMap<>();
    }
    public void incrementBlocksMined() {
        this.blocksMined++;
    }
    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }
    public void addVisit(UUID visitor) {
        this.visits++;
        visitorStats.put(visitor, visitorStats.getOrDefault(visitor, 0) + 1);
        try {
            fr.ju.privateMines.PrivateMines plugin = fr.ju.privateMines.PrivateMines.getInstance();
            if (plugin != null) {
                plugin.getLogger().info("[DEBUG] MineStats.addVisit: Visit recorded for UUID " + visitor + 
                                      " to mine owned by " + mineOwner + 
                                      ". Total visits: " + visits + 
                                      ". This visitor's visits: " + visitorStats.get(visitor));
            }
        } catch (Exception e) {
            if (fr.ju.privateMines.PrivateMines.getInstance() != null) {
                fr.ju.privateMines.PrivateMines.getInstance().getLogger().severe("[ERROR] MineStats.addVisit: Error logging visit: " + e.getMessage());
            }
        }
    }
    public void resetBlockStats() {
        this.blocksMined = 0;
        this.lastReset = System.currentTimeMillis();
    }
    public int getPercentageMined() {
        if (totalBlocks <= 0) return 0;
        double percentage = (blocksMined * 100.0) / totalBlocks;
        return Math.min(100, (int) Math.round(percentage));
    }
    public boolean shouldAutoReset(int threshold) {
        return getPercentageMined() >= threshold;
    }
    public UUID getMineOwner() {
        return mineOwner;
    }
    public int getBlocksMined() {
        return blocksMined;
    }
    public void setBlocksMined(int blocksMined) {
        this.blocksMined = blocksMined;
    }
    public int getTotalBlocks() {
        return totalBlocks;
    }
    public int getVisits() {
        return visits;
    }
    public long getLastReset() {
        return lastReset;
    }
    public Map<UUID, Integer> getVisitorStats() {
        return new HashMap<>(visitorStats);
    }
    public void setLastReset(long lastReset) {
        this.lastReset = lastReset;
    }
} 