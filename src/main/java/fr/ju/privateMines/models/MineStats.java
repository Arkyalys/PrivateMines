package fr.ju.privateMines.models;
import java.util.Collections;
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
        return Collections.unmodifiableMap(visitorStats);
    }
    public void setLastReset(long lastReset) {
        this.lastReset = lastReset;
    }
} 