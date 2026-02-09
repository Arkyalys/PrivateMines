package fr.ju.privateMines.models;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
public class MineStats {
    private final UUID mineOwner;
    private final AtomicInteger blocksMined = new AtomicInteger(0);
    private volatile int totalBlocks;
    private final AtomicInteger visits = new AtomicInteger(0);
    private volatile long lastReset;
    private final Map<UUID, Integer> visitorStats;
    public MineStats(UUID mineOwner) {
        this.mineOwner = mineOwner;
        this.totalBlocks = 0;
        this.lastReset = System.currentTimeMillis();
        this.visitorStats = new ConcurrentHashMap<>();
    }
    public void incrementBlocksMined() {
        this.blocksMined.incrementAndGet();
    }
    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }
    public void addVisit(UUID visitor) {
        this.visits.incrementAndGet();
        visitorStats.merge(visitor, 1, Integer::sum);
    }
    public void resetBlockStats() {
        this.blocksMined.set(0);
        this.lastReset = System.currentTimeMillis();
    }
    public int getPercentageMined() {
        int total = totalBlocks;
        if (total <= 0) return 0;
        double percentage = (blocksMined.get() * 100.0) / total;
        return Math.min(100, (int) Math.round(percentage));
    }
    public boolean shouldAutoReset(int threshold) {
        return getPercentageMined() >= threshold;
    }
    public UUID getMineOwner() {
        return mineOwner;
    }
    public int getBlocksMined() {
        return blocksMined.get();
    }
    public void setBlocksMined(int blocksMined) {
        this.blocksMined.set(blocksMined);
    }
    public int getTotalBlocks() {
        return totalBlocks;
    }
    public int getVisits() {
        return visits.get();
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