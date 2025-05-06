package fr.ju.privateMines.listeners;
import org.bukkit.Location;
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
        // Suppression de la logique d'ajout de visite
    }
    private Mine findMineByLocation(Location location) {
        // Vérifier le cache d'abord
        Mine cachedResult = getCachedMine(location);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Si pas dans le cache, chercher parmi toutes les mines
        Mine foundMine = findMineByCoordinates(location);
        
        // Mettre en cache le résultat
        updateMineCache(location, foundMine);
        
        return foundMine;
    }
    
    /**
     * Vérifie si la mine pour cette location est déjà en cache
     */
    private Mine getCachedMine(Location location) {
        if (plugin.getCacheManager() == null) {
            return null;
        }
        
        String cacheKey = buildLocationCacheKey(location);
        Object cachedMine = plugin.getCacheManager().get(cacheKey);
        
        if (cachedMine instanceof Mine) {
            return (Mine) cachedMine;
        }
        
        if (cachedMine instanceof Boolean && !(Boolean)cachedMine) {
            return null;
        }
        
        return null;
    }
    
    /**
     * Cherche une mine contenant la location spécifiée
     */
    private Mine findMineByCoordinates(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        
        for (Mine mine : mineManager.getAllMines()) {
            if (isLocationInMine(location, mine)) {
                return mine;
            }
        }
        
        return null;
    }
    
    /**
     * Vérifie si une location est dans les limites d'une mine
     */
    private boolean isLocationInMine(Location location, Mine mine) {
        if (!mine.hasMineArea()) {
            return false;
        }
        
        if (!location.getWorld().equals(mine.getLocation().getWorld())) {
            return false;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        return x >= mine.getMinX() && x <= mine.getMaxX() &&
               y >= mine.getMinY() && y <= mine.getMaxY() &&
               z >= mine.getMinZ() && z <= mine.getMaxZ();
    }
    
    /**
     * Met à jour le cache avec le résultat de la recherche
     */
    private void updateMineCache(Location location, Mine foundMine) {
        if (plugin.getCacheManager() == null) {
            return;
        }
        
        String cacheKey = buildLocationCacheKey(location);
        Object valueToCache = foundMine != null ? foundMine : Boolean.FALSE;
        
        // 5 minutes (300 secondes) de cache
        plugin.getCacheManager().put(cacheKey, valueToCache, 300);
    }
    
    /**
     * Construit la clé de cache pour une location
     */
    private String buildLocationCacheKey(Location location) {
        return "mine_at_" + location.getWorld().getName() + "_" + 
               location.getBlockX() + "_" + 
               location.getBlockY() + "_" + 
               location.getBlockZ();
    }
} 