package fr.ju.privateMines.api;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineAccess;

public class PrivateMinesAPI {
    private static PrivateMinesAPI instance;
    private final PrivateMines plugin;
    private PrivateMinesAPI(PrivateMines plugin) {
        this.plugin = plugin;
    }
    public static void init(PrivateMines plugin) {
        if (instance == null) {
            instance = new PrivateMinesAPI(plugin);
        }
    }
    public static PrivateMinesAPI getInstance() {
        if (instance == null) throw new IllegalStateException("L'API PrivateMines n'a pas été initialisée");
        return instance;
    }
    public PrivateMines getPlugin() { return plugin; }

    // Mines
    public boolean hasMine(Player player) { return plugin.getMineManager().hasMine(player); }
    public boolean hasMine(UUID uuid) { return plugin.getMineManager().hasMine(uuid); }
    public Mine getMine(Player player) { return plugin.getMineManager().getMine(player).orElse(null); }
    public Mine getMine(UUID uuid) { return plugin.getMineManager().getMine(uuid).orElse(null); }
    public Collection<Mine> getAllMines() { return plugin.getMineManager().getAllMines(); }
    public Map<UUID, Mine> getPlayerMines() { return plugin.getMineManager().mineMemoryService.getPlayerMines(); }
    public List<Mine> getPublicMines() { return plugin.getMineManager().getAllMines().stream().filter(Mine::isOpen).toList(); }
    public boolean hasMineAt(Location location) { return getMineAtLocation(location) != null; }
    public Mine createMine(Player player) { plugin.getMineManager().createMine(player); return getMine(player); }
    public boolean deleteMine(Player player) { return plugin.getMineManager().deleteMine(player); }
    public boolean deleteMine(UUID uuid) { Player p = plugin.getServer().getPlayer(uuid); return p != null && plugin.getMineManager().deleteMine(p); }
    public boolean resetMine(Player player) { 
        plugin.getMineManager().resetMine(player); 
        Mine mine = getMine(player);
        if (mine != null) {
            Location loc = plugin.getMineManager().getBetterTeleportLocation(mine);
            player.teleport(loc);
        }
        return true; 
    }
    public boolean resetMine(UUID uuid) { 
        plugin.getMineManager().resetMine(uuid); 
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Mine mine = getMine(uuid);
            if (mine != null) {
                Location loc = plugin.getMineManager().getBetterTeleportLocation(mine);
                player.teleport(loc);
            }
        }
        return true; 
    }
    public boolean expandMine(Player player) { return plugin.getMineManager().expandMine(player); }
    public boolean expandMine(Player player, int expandSize) { return plugin.getMineManager().expandMine(player, expandSize); }
    public boolean upgradeMine(Player player) { return plugin.getMineManager().upgradeMine(player); }
    public boolean setMineOpen(Player player, boolean isOpen) { Mine mine = getMine(player); if (mine == null) return false; mine.setOpen(isOpen); return true; }
    public boolean setMineTax(Player player, int tax) { return plugin.getMineManager().setMineTax(player, tax); }
    public int getMineTax(Player player) { Mine mine = getMine(player); return mine != null ? mine.getTax() : -1; }
    public int getMineTax(UUID uuid) { Mine mine = getMine(uuid); return mine != null ? mine.getTax() : -1; }
    public Location getTeleportLocation(Player player) { Mine mine = getMine(player); return mine != null ? mine.getTeleportLocation() : null; }
    public boolean setTeleportLocation(Player player, Location location) { Mine mine = getMine(player); if (mine == null) return false; mine.setTeleportLocation(location); return true; }
    public boolean teleportPlayerToMine(Player player, Mine targetMine) { if (targetMine == null) return false; Location loc = targetMine.getTeleportLocation(); return loc != null && player.teleport(loc); }
    public Mine getMineAtLocation(Location location) { return plugin.getMineManager().getAllMines().stream().filter(m -> m.hasMineArea() && location.getWorld() != null && location.getWorld().equals(m.getLocation().getWorld()) && location.getBlockX() >= m.getMinX() && location.getBlockX() <= m.getMaxX() && location.getBlockY() >= m.getMinY() && location.getBlockY() <= m.getMaxY() && location.getBlockZ() >= m.getMinZ() && location.getBlockZ() <= m.getMaxZ()).findFirst().orElse(null); }
    public boolean isBlockFromPlayerMine(Location location, UUID uuid) { Mine mine = getMineAtLocation(location); return mine != null && mine.getOwner().equals(uuid); }
    public boolean isBlockFromPlayerMine(Location location, Player player) { return isBlockFromPlayerMine(location, player.getUniqueId()); }
    public boolean isLocationInMineArea(Location location, Mine mine) { if (!mine.hasMineArea() || location.getWorld() == null) return false; if (location.getWorld().equals(mine.getLocation().getWorld())) { int x = location.getBlockX(); int y = location.getBlockY(); int z = location.getBlockZ(); return x >= mine.getMinX() && x <= mine.getMaxX() && y >= mine.getMinY() && y <= mine.getMaxY() && z >= mine.getMinZ() && z <= mine.getMaxZ(); } return false; }
    public boolean canAccessMine(Player player, Mine mine) { return mine != null && mine.canPlayerAccess(player.getUniqueId()); }

    // Nouvelles méthodes pour l'enchantement Nuke
    /**
     * Récupère tous les blocs présents dans la mine
     * @param mine La mine pour laquelle récupérer les blocs
     * @return Une map contenant les blocs (Material) et leur position (Location)
     */
    public Map<Location, Material> getMineBlocks(Mine mine) {
        if (mine == null || !mine.hasMineArea() || mine.getLocation().getWorld() == null) {
            return new HashMap<>();
        }
        
        Map<Location, Material> blocks = new HashMap<>();
        World world = mine.getLocation().getWorld();
        
        for (int x = mine.getMinX(); x <= mine.getMaxX(); x++) {
            for (int y = mine.getMinY(); y <= mine.getMaxY(); y++) {
                for (int z = mine.getMinZ(); z <= mine.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    if (!material.isAir() && !material.equals(Material.BEDROCK)) {
                        blocks.put(block.getLocation(), material);
                    }
                }
            }
        }
        
        return blocks;
    }
    
    /**
     * Calcule le pourcentage de remplissage actuel de la mine
     * @param mine La mine pour laquelle calculer le remplissage
     * @return Le pourcentage de remplissage (0-100)
     */
    public float getMineFillRatio(Mine mine) {
        if (mine == null || !mine.hasMineArea()) {
            return 0;
        }
        
        int totalBlocks = getTotalBlockCount(mine);
        if (totalBlocks <= 0) {
            return 0;
        }
        
        int breakableBlocks = getBreakableBlockCount(mine);
        return (float) breakableBlocks / totalBlocks * 100;
    }
    
    /**
     * Calcule le nombre total théorique de blocs que peut contenir la mine
     * @param mine La mine pour laquelle calculer le nombre total de blocs
     * @return Le nombre total de blocs
     */
    public int getTotalBlockCount(Mine mine) {
        if (mine == null || !mine.hasMineArea()) {
            return 0;
        }
        
        return (mine.getMaxX() - mine.getMinX() + 1) * 
               (mine.getMaxY() - mine.getMinY() + 1) * 
               (mine.getMaxZ() - mine.getMinZ() + 1);
    }
    
    /**
     * Récupère le type principal de bloc de la mine
     * @param mine La mine pour laquelle récupérer le type de bloc
     * @return Le type principal de bloc de la mine
     */
    public Material getMineBlockType(Mine mine) {
        if (mine == null || mine.getBlocks() == null || mine.getBlocks().isEmpty()) {
            return Material.STONE;
        }
        
        Material mainBlock = Material.STONE;
        double maxProbability = 0;
        
        for (Map.Entry<Material, Double> entry : mine.getBlocks().entrySet()) {
            if (entry.getValue() > maxProbability) {
                maxProbability = entry.getValue();
                mainBlock = entry.getKey();
            }
        }
        
        return mainBlock;
    }
    
    /**
     * Compte le nombre de blocs cassables actuellement dans la mine
     * @param mine La mine pour laquelle compter les blocs cassables
     * @return Le nombre de blocs cassables
     */
    public int getBreakableBlockCount(Mine mine) {
        if (mine == null || !mine.hasMineArea() || mine.getLocation().getWorld() == null) {
            return 0;
        }
        
        int count = 0;
        World world = mine.getLocation().getWorld();
        
        for (int x = mine.getMinX(); x <= mine.getMaxX(); x++) {
            for (int y = mine.getMinY(); y <= mine.getMaxY(); y++) {
                for (int z = mine.getMinZ(); z <= mine.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    if (!material.isAir() && !material.equals(Material.BEDROCK)) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }

    // Accès et permissions
    public MineAccess getMineAccess(Player player) { Mine mine = getMine(player); return mine != null ? mine.getMineAccess() : null; }
    public List<Mine> getTopMines() { return plugin.getStatsManager().getTopMines(); }
    public void saveStats() { plugin.getStatsManager().saveStats(); }
    public void syncMineStats(Mine mine) { plugin.getStatsManager().syncMineStats(mine); }

    // Hologrammes
    public void createOrUpdateHologram(Mine mine) { if (plugin.getHologramManager() != null) plugin.getHologramManager().createOrUpdateHologram(mine); }
    public void removeHologram(UUID ownerId) { if (plugin.getHologramManager() != null) plugin.getHologramManager().removeHologram(ownerId); }
    public void updateAllHolograms() { if (plugin.getHologramManager() != null) plugin.getHologramManager().updateAllHolograms(); }

    // Monde des mines
    public World getMineWorld() { return plugin.getMineWorldManager().getMineWorld(); }
    public String getMineWorldName() { return plugin.getMineWorldManager().getMineWorldName(); }
    public Location getNextMineLocation() { return plugin.getMineWorldManager().getNextMineLocation(); }
    public void unloadMineWorld() { plugin.getMineWorldManager().unloadWorld(); }

    // GUIs
    public void openMainGUI(Player player) { fr.ju.privateMines.guis.MineMainGUI.openGUI(player); }
    public void openStatsGUI(Player player) { fr.ju.privateMines.guis.MineStatsGUI.openGUI(player); }
    public void openVisitorsGUI(Player player, int page) { fr.ju.privateMines.guis.MineVisitorsGUI.openGUI(player, page); }
    public void openSettingsGUI(Player player) { fr.ju.privateMines.guis.MineSettingsGUI.openGUI(player); }
    public void openExpandGUI(Player player) { fr.ju.privateMines.guis.MineExpandGUI.openGUI(player); }
    public void openCompositionGUI(Player player) { fr.ju.privateMines.guis.MineCompositionGUI.openGUI(player); }

    // Protection WorldGuard
    public void protectMine(Mine mine, com.sk89q.worldedit.math.BlockVector3[] bounds) { plugin.getMineManager().getMineProtectionManager().protectMine(mine, bounds); }
    public void unprotectMine(Mine mine) { plugin.getMineManager().getMineProtectionManager().unprotectMine(mine); }
    public void updateMineProtection(Mine mine) { plugin.getMineManager().getMineProtectionManager().updateMineProtection(mine); }

    // Types et tiers
    /**
     * Récupère la liste complète des paliers (tiers) de mines disponibles
     * @return Une map associant les numéros de palier avec leurs configurations de blocs
     */
    public Map<Integer, Map<Material, Double>> getMineTiers() { return plugin.getMineManager().getMineTiers(); }
    public void reloadMineTiers() { plugin.getMineManager().loadMineTiers(); }

    // Sauvegarde/chargement
    public void saveMineData(Player player) { plugin.getMineManager().saveMineData(player); }
    public void saveMine(Mine mine) { plugin.getMineManager().saveMine(mine); }
    public void loadMineData() { plugin.getMineManager().loadMineData(); }
    public void saveAllMineData() { plugin.getMineManager().saveAllMineData(); }
    public void reloadPlugin() { plugin.reloadPlugin(); }

    /**
     * Définit le palier (tier) de la mine d'un joueur et applique le reset.
     * @param player Le joueur cible
     * @param tier Le nouveau palier
     * @return true si la modification a réussi
     */
    public boolean setMineTier(Player player, int tier) {
        Mine mine = getMine(player);
        if (mine == null) return false;
        mine.setTier(tier);
        saveMine(mine);
        resetMine(player);
        return true;
    }
    /**
     * Définit le palier (tier) de la mine d'un joueur (par UUID) et applique le reset.
     * @param uuid L'UUID du joueur
     * @param tier Le nouveau palier
     * @return true si la modification a réussi
     */
    public boolean setMineTier(UUID uuid, int tier) {
        Mine mine = getMine(uuid);
        if (mine == null) return false;
        mine.setTier(tier);
        saveMine(mine);
        resetMine(uuid);
        return true;
    }
} 