package fr.ju.privateMines.managers;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.services.MineRegionService;
public class MineProtectionManager {
    private final String regionPrefix = "mine-";
    private final String fullMinePrefix = "fullmine-";
    private final PrivateMines plugin;
    private final MineRegionService mineRegionService;
    public MineProtectionManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineRegionService = new MineRegionService(plugin, this);
    }
    public void protectMine(Mine mine, com.sk89q.worldedit.math.BlockVector3[] schematicBounds) {
        mineRegionService.protectMine(mine, schematicBounds);
    }
    public void unprotectMine(Mine mine) {
        mineRegionService.unprotectMine(mine);
    }
    public void updateMineProtection(Mine mine) {
        mineRegionService.updateMineProtection(mine);
    }
    public boolean isPlayerInMineRegion(Player player) {
        if (player == null) {
            PrivateMines.debugLog("[DEBUG-REGION] Player est null, retourne false");
            return false;
        }
        World world = player.getWorld();
        if (world == null) {
            PrivateMines.debugLog("[DEBUG-REGION] Monde du joueur " + player.getName() + " est null, retourne false");
            return false;
        }
        PrivateMines.debugLog("[DEBUG-REGION] Vérification de région pour " + player.getName() + " dans le monde " + world.getName());
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            PrivateMines.debugLog("[DEBUG-REGION] RegionManager est null pour le monde " + world.getName() + ", retourne false");
            return false;
        }
        String mineRegionId = regionPrefix + player.getUniqueId().toString();
        String fullMineRegionId = fullMinePrefix + player.getUniqueId().toString();
        boolean mineRegionExists = regionManager.hasRegion(mineRegionId);
        if (!mineRegionExists) {
            PrivateMines.debugLog("[DEBUG-REGION] Aucune région mine trouvée pour le joueur, retourne false");
            return false;
        }
        boolean fullMineRegionExists = regionManager.hasRegion(fullMineRegionId);
        PrivateMines.debugLog("[DEBUG-REGION] Régions trouvées: mine=" + mineRegionExists + ", fullmine=" + fullMineRegionExists);
        com.sk89q.worldedit.util.Location worldEditLoc = BukkitAdapter.adapt(player.getLocation());
        com.sk89q.worldguard.protection.ApplicableRegionSet regions =
            WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .getApplicableRegions(worldEditLoc);
        boolean isInMineRegion = regions.getRegions().stream()
            .anyMatch(region -> region.getId().equals(mineRegionId));
        PrivateMines.debugLog("[DEBUG-REGION] Le joueur " + player.getName() + " est" + (isInMineRegion ? "" : " PAS") +
                              " dans sa région mine " + mineRegionId);
        return isInMineRegion;
    }
    public String getMineRegionId(Player player) {
        return regionPrefix + player.getUniqueId().toString();
    }
    public boolean addMemberToMineRegion(UUID owner, UUID member) {
        Mine mine = plugin.getMineManager().getMine(owner).orElse(null);
        if (mine == null) return false;
        World world = mine.getLocation().getWorld();
        if (world == null) return false;
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return false;
        String regionId = regionPrefix + owner.toString();
        ProtectedRegion region = regionManager.getRegion(regionId);
        if (region == null) return false;
        region.getMembers().addPlayer(member);
        return true;
    }
    public boolean removeMemberFromMineRegion(UUID owner, UUID member) {
        Mine mine = plugin.getMineManager().getMine(owner).orElse(null);
        if (mine == null) return false;
        World world = mine.getLocation().getWorld();
        if (world == null) return false;
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) return false;
        String regionId = regionPrefix + owner.toString();
        ProtectedRegion region = regionManager.getRegion(regionId);
        if (region == null) return false;
        region.getMembers().removePlayer(member);
        return true;
    }
} 