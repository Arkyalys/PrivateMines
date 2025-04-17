package fr.ju.privateMines.managers;
import org.bukkit.World;
import org.bukkit.entity.Player;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
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
            plugin.getLogger().info("[DEBUG-REGION] Player est null, retourne false");
            return false;
        }
        World world = player.getWorld();
        if (world == null) {
            plugin.getLogger().info("[DEBUG-REGION] Monde du joueur " + player.getName() + " est null, retourne false");
            return false;
        }
        plugin.getLogger().info("[DEBUG-REGION] Vérification de région pour " + player.getName() + " dans le monde " + world.getName());
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            plugin.getLogger().info("[DEBUG-REGION] RegionManager est null pour le monde " + world.getName() + ", retourne false");
            return false;
        }
        String mineRegionId = regionPrefix + player.getUniqueId().toString();
        String fullMineRegionId = fullMinePrefix + player.getUniqueId().toString();
        plugin.getLogger().info("[DEBUG-REGION] Recherche des régions: " + mineRegionId + " et " + fullMineRegionId);
        boolean mineRegionExists = regionManager.hasRegion(mineRegionId);
        boolean fullMineRegionExists = regionManager.hasRegion(fullMineRegionId);
        if (!mineRegionExists) {
            plugin.getLogger().info("[DEBUG-REGION] Aucune région mine trouvée pour le joueur, retourne false");
            return false;
        }
        plugin.getLogger().info("[DEBUG-REGION] Régions trouvées: mine=" + mineRegionExists + ", fullmine=" + fullMineRegionExists);
        com.sk89q.worldedit.util.Location worldEditLoc = BukkitAdapter.adapt(player.getLocation());
        plugin.getLogger().info("[DEBUG-REGION] Position du joueur: " + player.getLocation().getBlockX() + ", " + 
                              player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
        com.sk89q.worldguard.protection.ApplicableRegionSet regions = 
            WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .getApplicableRegions(worldEditLoc);
        plugin.getLogger().info("[DEBUG-REGION] Régions trouvées à la position du joueur:");
        regions.getRegions().forEach(region -> 
            plugin.getLogger().info("[DEBUG-REGION] - " + region.getId())
        );
        boolean isInMineRegion = regions.getRegions().stream()
            .anyMatch(region -> region.getId().equals(mineRegionId));
        boolean isInFullMineRegion = regions.getRegions().stream()
            .anyMatch(region -> region.getId().equals(fullMineRegionId));
        plugin.getLogger().info("[DEBUG-REGION] Le joueur " + player.getName() + " est" + (isInMineRegion ? "" : " PAS") + 
                              " dans sa région mine " + mineRegionId);
        plugin.getLogger().info("[DEBUG-REGION] Le joueur " + player.getName() + " est" + (isInFullMineRegion ? "" : " PAS") + 
                              " dans sa région fullmine " + fullMineRegionId);
        return isInMineRegion;
    }
    public String getMineRegionId(Player player) {
        return regionPrefix + player.getUniqueId().toString();
    }
} 