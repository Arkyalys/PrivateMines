package fr.ju.privateMines.listeners;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
public class MineStatsListener implements Listener {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    private final StatsManager statsManager;
    public MineStatsListener(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineManager = plugin.getMineManager();
        this.statsManager = plugin.getStatsManager();
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        Mine mine = findMineByLocation(blockLoc);
        if (mine == null) return;
        statsManager.incrementBlocksMined(mine);
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createOrUpdateHologram(mine);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && 
            event.getFrom().getBlockY() == event.getTo().getBlockY() && 
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        Location loc = event.getTo();
        Mine mine = findMineByLocation(loc);
        if (mine == null) return;
        if (!mine.getOwner().equals(player.getUniqueId())) {
            statsManager.addVisit(mine, player.getUniqueId());
        }
    }
    private Mine findMineByLocation(Location location) {
        if (plugin.getCacheManager() != null) {
            Object cachedMine = plugin.getCacheManager().get("mine_at_" + location.getWorld().getName() + "_" + 
                                                           location.getBlockX() + "_" + 
                                                           location.getBlockY() + "_" + 
                                                           location.getBlockZ());
            if (cachedMine instanceof Mine) {
                return (Mine) cachedMine;
            }
            if (cachedMine instanceof Boolean && !(Boolean)cachedMine) {
                return null;
            }
        }
        for (Mine mine : mineManager.getAllMines()) {
            if (!mine.hasMineArea()) continue;
            if (location.getWorld() != null && location.getWorld().equals(mine.getLocation().getWorld())) {
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();
                if (x >= mine.getMinX() && x <= mine.getMaxX() &&
                    y >= mine.getMinY() && y <= mine.getMaxY() &&
                    z >= mine.getMinZ() && z <= mine.getMaxZ()) {
                    if (plugin.getCacheManager() != null) {
                        plugin.getCacheManager().put("mine_at_" + location.getWorld().getName() + "_" + 
                                                   location.getBlockX() + "_" + 
                                                   location.getBlockY() + "_" + 
                                                   location.getBlockZ(), mine, 300); 
                    }
                    return mine;
                }
            }
        }
        if (plugin.getCacheManager() != null) {
            plugin.getCacheManager().put("mine_at_" + location.getWorld().getName() + "_" + 
                                       location.getBlockX() + "_" + 
                                       location.getBlockY() + "_" + 
                                       location.getBlockZ(), false, 300); 
        }
        return null;
    }
} 