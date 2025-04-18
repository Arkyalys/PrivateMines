package fr.ju.privateMines.services;
import org.bukkit.Location;
import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineProtectionManager;
import fr.ju.privateMines.models.Mine;
public class MineRegionService {
    private final PrivateMines plugin;
    private final MineProtectionManager mineProtectionManager;
    private final String regionPrefix = "mine-";
    private final String fullMinePrefix = "fullmine-";
    public MineRegionService(PrivateMines plugin, MineProtectionManager mineProtectionManager) {
        this.plugin = plugin;
        this.mineProtectionManager = mineProtectionManager;
    }
    public void protectMine(Mine mine, BlockVector3[] schematicBounds) {
        World world = mine.getLocation().getWorld();
        if (world == null) return;
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return;
        String regionId = regionPrefix + mine.getOwner().toString();
        String fullMineId = fullMinePrefix + mine.getOwner().toString();
        BlockVector3 min, max;
        if (mine.hasMineArea()) {
            min = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ());
            max = BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ());
        } else {
            int size = mine.getSize();
            Location center = mine.getLocation();
            min = BlockVector3.at(center.getX() - size, center.getY(), center.getZ() - size);
            max = BlockVector3.at(center.getX() + size, center.getY() + size, center.getZ() + size);
        }
        ProtectedRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(1);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.USE, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.getOwners().addPlayer(mine.getOwner());
        regionManager.addRegion(region);
        BlockVector3[] boundsToUse = schematicBounds;
        if ((boundsToUse == null || boundsToUse.length < 2 || boundsToUse[0] == null || boundsToUse[1] == null) && mine.hasSchematicBounds()) {
            BlockVector3 minS = BlockVector3.at(mine.getSchematicMinX(), mine.getSchematicMinY(), mine.getSchematicMinZ());
            BlockVector3 maxS = BlockVector3.at(mine.getSchematicMaxX(), mine.getSchematicMaxY(), mine.getSchematicMaxZ());
            boundsToUse = new BlockVector3[] { minS, maxS };
        }
        boolean schematicValide = false;
        if (boundsToUse != null && boundsToUse.length >= 2 && boundsToUse[0] != null && boundsToUse[1] != null) {
            BlockVector3 minS = boundsToUse[0];
            BlockVector3 maxS = boundsToUse[1];
            if (!(minS.getBlockX() == 0 && minS.getBlockY() == 0 && minS.getBlockZ() == 0 &&
                  maxS.getBlockX() == 0 && maxS.getBlockY() == 0 && maxS.getBlockZ() == 0)) {
                schematicValide = true;
                ProtectedRegion fullMineRegion = new ProtectedCuboidRegion(fullMineId, minS, maxS);
                fullMineRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.USE, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.INTERACT, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.PVP, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
                fullMineRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
                regionManager.addRegion(fullMineRegion);
                plugin.getLogger().info("[DEBUG-CREATE] Région fullmine-" + mine.getOwner() + " créée : min=" + minS + ", max=" + maxS);
            }
        }
        if (!schematicValide && mine.hasSchematicBounds()) {
            plugin.getLogger().warning("[DEBUG-CREATE] Impossible de créer la région fullmine-" + mine.getOwner() + " : bounds schematic invalides ou absents (vérifiez data.yml)");
        }
    }
    public void unprotectMine(Mine mine) {
        World world = mine.getLocation().getWorld();
        if (world == null) return;
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return;
        String regionId = regionPrefix + mine.getOwner().toString();
        String fullMineId = fullMinePrefix + mine.getOwner().toString();
        regionManager.removeRegion(regionId);
        regionManager.removeRegion(fullMineId);
    }
    public void updateMineProtection(Mine mine) {
        unprotectMine(mine);
        if (mine.hasSchematicBounds()) {
            BlockVector3 min = BlockVector3.at(mine.getSchematicMinX(), mine.getSchematicMinY(), mine.getSchematicMinZ());
            BlockVector3 max = BlockVector3.at(mine.getSchematicMaxX(), mine.getSchematicMaxY(), mine.getSchematicMaxZ());
            BlockVector3[] schematicBounds = new BlockVector3[] { min, max };
            protectMine(mine, schematicBounds);
        } else {
            protectMine(mine, null);
        }
    }
} 