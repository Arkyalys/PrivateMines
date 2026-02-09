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
    private final String regionPrefix = "mine-";
    private final String fullMinePrefix = "fullmine-";
    public MineRegionService(PrivateMines plugin, MineProtectionManager mineProtectionManager) {
        this.plugin = plugin;
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
        ProtectedRegion region = createProtectedRegion(regionId, min, max, mine);
        regionManager.addRegion(region);
        BlockVector3[] boundsToUse = resolveSchematicBounds(mine, schematicBounds);
        boolean schematicValide = false;
        if (isValidSchematicBounds(boundsToUse)) {
            schematicValide = true;
            ProtectedRegion fullMineRegion = createFullMineRegion(fullMineId, boundsToUse);
            regionManager.addRegion(fullMineRegion);
            PrivateMines.debugLog("[DEBUG-CREATE] Région fullmine-" + mine.getOwner() + " créée : min=" + boundsToUse[0] + ", max=" + boundsToUse[1]);
        }
        if (!schematicValide && mine.hasSchematicBounds()) {
            plugin.getLogger().warning("[DEBUG-CREATE] Impossible de créer la région fullmine-" + mine.getOwner() + " : bounds schematic invalides ou absents (vérifiez data.yml)");
        }
    }
    private ProtectedRegion createProtectedRegion(String regionId, BlockVector3 min, BlockVector3 max, Mine mine) {
        ProtectedRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(10);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.USE, StateFlag.State.ALLOW);
        region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
        region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
        com.sk89q.worldguard.protection.flags.Flag<?> invincibleFlagRaw = WorldGuard.getInstance().getFlagRegistry().get("invincible");
        if (invincibleFlagRaw instanceof StateFlag invincibleFlag) {
            region.setFlag(invincibleFlag, StateFlag.State.ALLOW);
        } else if (invincibleFlagRaw != null) {
            plugin.getLogger().warning("Le flag 'invincible' existe mais n'est pas un StateFlag !");
        }
        com.sk89q.worldguard.protection.flags.Flag<?> upcEnchantsFlagRaw = WorldGuard.getInstance().getFlagRegistry().get("upc-enchants");
        if (upcEnchantsFlagRaw instanceof StateFlag upcEnchantsFlag) {
            region.setFlag(upcEnchantsFlag, StateFlag.State.ALLOW);
        } else if (upcEnchantsFlagRaw != null) {
            plugin.getLogger().warning("Le flag 'upc-enchants' existe mais n'est pas un StateFlag !");
        }
        region.getOwners().addPlayer(mine.getOwner());
        return region;
    }
    private BlockVector3[] resolveSchematicBounds(Mine mine, BlockVector3[] schematicBounds) {
        BlockVector3[] boundsToUse = schematicBounds;
        if ((boundsToUse == null || boundsToUse.length < 2 || boundsToUse[0] == null || boundsToUse[1] == null) && mine.hasSchematicBounds()) {
            BlockVector3 minS = BlockVector3.at(mine.getSchematicMinX(), mine.getSchematicMinY(), mine.getSchematicMinZ());
            BlockVector3 maxS = BlockVector3.at(mine.getSchematicMaxX(), mine.getSchematicMaxY(), mine.getSchematicMaxZ());
            boundsToUse = new BlockVector3[] { minS, maxS };
        }
        return boundsToUse;
    }
    /**
     * Vérifie si les limites du schéma sont valides.
     */
    private boolean isValidSchematicBounds(BlockVector3[] boundsToUse) {
        if (!hasValidBoundsStructure(boundsToUse)) {
            return false;
        }
        
        return !isOriginBounds(boundsToUse[0], boundsToUse[1]);
    }
    /**
     * Vérifie si le tableau de limites a une structure valide (non null, longueur suffisante, éléments non null).
     */
    private boolean hasValidBoundsStructure(BlockVector3[] bounds) {
        return bounds != null && 
               bounds.length >= 2 && 
               bounds[0] != null && 
               bounds[1] != null;
    }
    /**
     * Vérifie si les deux points sont à l'origine (0,0,0), ce qui indiquerait des limites non valides.
     */
    private boolean isOriginBounds(BlockVector3 min, BlockVector3 max) {
        return isOriginPoint(min) && isOriginPoint(max);
    }
    /**
     * Vérifie si un point est à l'origine (0,0,0).
     */
    private boolean isOriginPoint(BlockVector3 point) {
        return point.getBlockX() == 0 && 
               point.getBlockY() == 0 && 
               point.getBlockZ() == 0;
    }
    private ProtectedRegion createFullMineRegion(String fullMineId, BlockVector3[] boundsToUse) {
        BlockVector3 minS = boundsToUse[0];
        BlockVector3 maxS = boundsToUse[1];
        ProtectedRegion fullMineRegion = new ProtectedCuboidRegion(fullMineId, minS, maxS);
        fullMineRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.USE, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.INTERACT, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.PVP, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
        fullMineRegion.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
        fullMineRegion.setPriority(-1);
        return fullMineRegion;
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