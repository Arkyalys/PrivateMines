package fr.ju.privateMines.models;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
public class Mine {
    private final UUID owner;
    private Location location;
    private String type;
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
    private final Set<UUID> contributors = new HashSet<>();
    public Mine(UUID owner, Location location, String type) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.owner = owner;
        this.location = location;
        this.type = type;
        this.size = 1;
        this.tax = 0;
        this.isOpen = true;
        this.blocks = new HashMap<>();
        this.teleportLocation = location.clone().add(0.5, 1, 0.5);
        this.teleportLocation.setYaw(270);
        this.stats = new MineStats(owner);
        this.tier = 1;
        this.mineAccess = new MineAccess(owner);
    }
    public UUID getOwner() {
        return owner;
    }
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
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
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
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
        if (fr.ju.privateMines.PrivateMines.getInstance() != null && 
            fr.ju.privateMines.PrivateMines.getInstance().getStatsManager() != null) {
            return fr.ju.privateMines.PrivateMines.getInstance().getStatsManager().incrementBlocksMined(this);
        }
        stats.incrementBlocksMined();
        return stats.shouldAutoReset(autoResetThreshold);
    }
    public void addVisit(UUID visitor) {
        fr.ju.privateMines.PrivateMines plugin = fr.ju.privateMines.PrivateMines.getInstance();
        plugin.getLogger().info("[DEBUG] Mine.addVisit: Adding visit from " + visitor + " to mine owned by " + owner);
        if (plugin != null && plugin.getStatsManager() != null) {
            plugin.getLogger().info("[DEBUG] Mine.addVisit: StatsManager found, delegating visit recording");
            plugin.getStatsManager().addVisit(this, visitor);
            return;
        }
        plugin.getLogger().info("[DEBUG] Mine.addVisit: StatsManager not available, using internal implementation");
        stats.addVisit(visitor);
        plugin.getLogger().info("[DEBUG] Mine.addVisit: Visit recorded using internal implementation. Total visits: " + stats.getVisits());
    }
    public void calculateTotalBlocks() {
        if (!hasMineArea()) return;
        int total = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        stats.setTotalBlocks(total);
        if (fr.ju.privateMines.PrivateMines.getInstance() != null && 
            fr.ju.privateMines.PrivateMines.getInstance().getStatsManager() != null) {
            fr.ju.privateMines.models.MineStats statsManagerStats = 
                fr.ju.privateMines.PrivateMines.getInstance().getStatsManager().getStats(owner);
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
        if (fr.ju.privateMines.PrivateMines.getInstance() != null && 
            fr.ju.privateMines.PrivateMines.getInstance().getStatsManager() != null) {
            fr.ju.privateMines.PrivateMines.getInstance().getStatsManager().syncMineStats(this);
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