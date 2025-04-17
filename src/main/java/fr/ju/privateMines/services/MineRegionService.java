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
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.USE, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        region.getOwners().addPlayer(mine.getOwner());
        regionManager.addRegion(region);
        if (schematicBounds != null && schematicBounds.length >= 2 && schematicBounds[0] != null && schematicBounds[1] != null) {
            BlockVector3 fullMin = schematicBounds[0];
            BlockVector3 fullMax = schematicBounds[1];
            ProtectedRegion fullMineRegion = new ProtectedCuboidRegion(fullMineId, fullMin, fullMax);
            fullMineRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.USE, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.INTERACT, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.PVP, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
            fullMineRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
            regionManager.addRegion(fullMineRegion);
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