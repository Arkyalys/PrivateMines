package fr.ju.privateMines.models;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;

import fr.ju.privateMines.services.IStatsService;
public class Mine {
    private UUID owner;
    private Location location;
    private int size;
    private int tax;
    private boolean isOpen;
    private Map<Material, Double> blocks;
    private Location teleportLocation;
    private int tier;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private MineStats stats;
    private double schematicMinX, schematicMinY, schematicMinZ;
    private double schematicMaxX, schematicMaxY, schematicMaxZ;
    private MineAccess mineAccess;
    private final Set<UUID> contributors = ConcurrentHashMap.newKeySet();
    private IStatsService statsService;
    
    public Mine(UUID owner, Location location) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        this.owner = owner;
        this.location = location;
        this.size = 1;
        this.tax = 0;
        this.isOpen = true;
        this.blocks = new ConcurrentHashMap<>();
        this.teleportLocation = location.clone().add(0.5, 1, 0.5);
        this.teleportLocation.setYaw(270);
        this.stats = new MineStats(owner);
        this.tier = 1;
        this.mineAccess = new MineAccess(owner);
    }
    
    /**
     * Constructeur avec service de statistiques.
     */
    public Mine(UUID owner, Location location, IStatsService statsService) {
        this(owner, location);
        this.statsService = statsService;
    }
    
    /**
     * Définit le service de statistiques.
     */
    public void setStatsService(IStatsService statsService) {
        this.statsService = statsService;
    }
    
    public UUID getOwner() {
        return owner;
    }
    public void setOwner(UUID owner) {
        this.owner = owner;
    }
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public int getTax() {
        return tax;
    }
    public void setTax(int tax) {
        this.tax = tax;
    }
    public boolean isOpen() {
        return isOpen;
    }
    public void setOpen(boolean open) {
        isOpen = open;
    }
    public Map<Material, Double> getBlocks() {
        return blocks;
    }
    public void setBlocks(Map<Material, Double> blocks) {
        this.blocks = blocks;
    }
    public Location getTeleportLocation() {
        return teleportLocation;
    }
    public void setTeleportLocation(Location teleportLocation) {
        this.teleportLocation = teleportLocation;
    }
    public void setMineArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        int totalBlocks = (int) Math.min(
            (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1),
            Integer.MAX_VALUE);
        this.stats.setTotalBlocks(totalBlocks);
        this.stats.resetBlockStats();
    }
    public int getMinX() {
        return minX;
    }
    public int getMinY() {
        return minY;
    }
    public int getMinZ() {
        return minZ;
    }
    public int getMaxX() {
        return maxX;
    }
    public int getMaxY() {
        return maxY;
    }
    public int getMaxZ() {
        return maxZ;
    }
    public boolean hasMineArea() {
        return minX != 0 || minY != 0 || minZ != 0 || maxX != 0 || maxY != 0 || maxZ != 0;
    }
    public boolean hasSchematicBounds() {
        return schematicMinX != 0 || schematicMinY != 0 || schematicMinZ != 0 || 
               schematicMaxX != 0 || schematicMaxY != 0 || schematicMaxZ != 0;
    }
    public void setSchematicBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.schematicMinX = minX;
        this.schematicMinY = minY;
        this.schematicMinZ = minZ;
        this.schematicMaxX = maxX;
        this.schematicMaxY = maxY;
        this.schematicMaxZ = maxZ;
    }
    public double getSchematicMinX() {
        return schematicMinX;
    }
    public double getSchematicMinY() {
        return schematicMinY;
    }
    public double getSchematicMinZ() {
        return schematicMinZ;
    }
    public double getSchematicMaxX() {
        return schematicMaxX;
    }
    public double getSchematicMaxY() {
        return schematicMaxY;
    }
    public double getSchematicMaxZ() {
        return schematicMaxZ;
    }
    public void expand() {
        this.size++;
    }
    public void incrementSize(int amount) {
        this.size += amount;
    }
    public void setLastResetTime(long timestamp) {
        if (stats != null) {
            stats.setLastReset(timestamp);
        }
    }
    public void reset() {
        if (stats != null) {
            stats.resetBlockStats();
        }
        calculateTotalBlocks();
    }
    public MineStats getStats() {
        return stats;
    }
    public boolean incrementBlocksMined(int autoResetThreshold) {
        if (statsService != null) {
            return statsService.incrementBlocksMined(this);
        }
        // Fallback si aucun service n'est disponible
        stats.incrementBlocksMined();
        return stats.shouldAutoReset(autoResetThreshold);
    }
    public void calculateTotalBlocks() {
        if (!hasMineArea()) return;
        int total = (int) Math.min(
            (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1),
            Integer.MAX_VALUE);
        stats.setTotalBlocks(total);
        if (statsService != null) {
            MineStats statsManagerStats = statsService.getStats(owner);
            if (statsManagerStats != null) {
                statsManagerStats.setTotalBlocks(total);
            }
        }
    }
    public int getTier() {
        return tier;
    }
    public void setTier(int tier) {
        this.tier = tier;
    }
    public void synchronizeStats() {
        if (statsService != null) {
            statsService.syncMineStats(this);
        }
    }
    public MineAccess getMineAccess() {
        if (mineAccess == null) {
            mineAccess = new MineAccess(owner);
        }
        return mineAccess;
    }
    public boolean canPlayerAccess(UUID playerUUID) {
        return getMineAccess().canAccess(playerUUID);
    }
    public Set<UUID> getContributors() {
        return new HashSet<>(contributors);
    }
    public void addContributor(UUID uuid) {
        if (uuid != null && !uuid.equals(owner)) {
            contributors.add(uuid);
        }
    }
    public void removeContributor(UUID uuid) {
        contributors.remove(uuid);
    }
    public boolean isContributor(UUID uuid) {
        return contributors.contains(uuid);
    }
} 