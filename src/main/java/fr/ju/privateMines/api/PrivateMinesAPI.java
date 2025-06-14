package fr.ju.privateMines.api;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineAccess;

/**
 * Implémentation concrète de l'API publique des mines privées.
 */
public class PrivateMinesAPI implements IPrivateMinesAPI {
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
    @Override
    public boolean hasMine(Player player) { return plugin.getMineManager().hasMine(player); }
    @Override
    public boolean hasMine(UUID uuid) { return plugin.getMineManager().hasMine(uuid); }
    @Override
    public Mine getMine(Player player) { return plugin.getMineManager().getMine(player).orElse(null); }
    @Override
    public Mine getMine(UUID uuid) { return plugin.getMineManager().getMine(uuid).orElse(null); }
    @Override
    public Collection<Mine> getAllMines() { return plugin.getMineManager().getAllMines(); }
    @Override
    public Map<UUID, Mine> getPlayerMines() { return plugin.getMineManager().mineMemoryService.getPlayerMines(); }
    @Override
    public List<Mine> getPublicMines() { return plugin.getMineManager().getAllMines().stream().filter(Mine::isOpen).toList(); }
    
    @Override
    public boolean hasMineAt(Location location) { return getMineAtLocation(location) != null; }
    
    @Override
    public Mine createMine(Player player) { plugin.getMineManager().createMine(player); return getMine(player); }
    
    @Override
    public boolean deleteMine(Player player) { return plugin.getMineManager().deleteMine(player); }
    
    @Override
    public boolean deleteMine(UUID uuid) { Player p = plugin.getServer().getPlayer(uuid); return p != null && plugin.getMineManager().deleteMine(p); }
    
    @Override
    public boolean resetMine(Player player) { 
        plugin.getMineManager().resetMine(player); 
        Mine mine = getMine(player);
        if (mine != null) {
            Location loc = plugin.getMineManager().getBetterTeleportLocation(mine);
            player.teleport(loc);
        }
        return true; 
    }
    
    @Override
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
    
    @Override
    public boolean expandMine(Player player) { return plugin.getMineManager().expandMine(player); }
    
    @Override
    public boolean expandMine(Player player, int expandSize) { return plugin.getMineManager().expandMine(player, expandSize); }
    
    @Override
    public boolean upgradeMine(Player player) { return plugin.getMineManager().upgradeMine(player); }
    
    @Override
    public boolean setMineOpen(Player player, boolean isOpen) {
        Mine mine = getMine(player);
        if (mine == null) return false;
        mine.setOpen(isOpen);
        plugin.getMetricsService().updateOpenMines((int) plugin.getMineManager().getAllMines().stream().filter(Mine::isOpen).count());
        return true;
    }
    
    @Override
    public boolean setMineTax(Player player, int tax) { return plugin.getMineManager().setMineTax(player, tax); }
    
    @Override
    public int getMineTax(Player player) { Mine mine = getMine(player); return mine != null ? mine.getTax() : -1; }
    
    @Override
    public int getMineTax(UUID uuid) { Mine mine = getMine(uuid); return mine != null ? mine.getTax() : -1; }
    
    @Override
    public Location getTeleportLocation(Player player) { Mine mine = getMine(player); return mine != null ? mine.getTeleportLocation() : null; }
    
    @Override
    public boolean setTeleportLocation(Player player, Location location) { Mine mine = getMine(player); if (mine == null) return false; mine.setTeleportLocation(location); return true; }
    
    @Override
    public boolean teleportPlayerToMine(Player player, Mine targetMine) { if (targetMine == null) return false; Location loc = targetMine.getTeleportLocation(); return loc != null && player.teleport(loc); }
    
    @Override
    public Mine getMineAtLocation(Location location) { 
        return plugin.getMineManager().getAllMines().stream()
            .filter(mine -> isLocationInMineArea(location, mine))
            .findFirst()
            .orElse(null); 
    }
    
    @Override
    public boolean isLocationInMineArea(Location location, Mine mine) { 
        if (!isValidMineAndLocation(mine, location)) {
            return false;
        }
        
        if (!areSameWorld(location, mine)) {
            return false;
        }
        
        return isWithinMineBounds(location, mine);
    }
    
    private boolean isValidMineAndLocation(Mine mine, Location location) {
        return mine.hasMineArea() && location.getWorld() != null;
    }
    
    private boolean areSameWorld(Location location, Mine mine) {
        return location.getWorld().equals(mine.getLocation().getWorld());
    }
    
    private boolean isWithinMineBounds(Location location, Mine mine) {
        int x = location.getBlockX();
        int y = location.getBlockY(); 
        int z = location.getBlockZ();
        
        return x >= mine.getMinX() && x <= mine.getMaxX() 
            && y >= mine.getMinY() && y <= mine.getMaxY() 
            && z >= mine.getMinZ() && z <= mine.getMaxZ();
    }
    
    @Override
    public boolean isBlockFromPlayerMine(Location location, UUID uuid) { Mine mine = getMineAtLocation(location); return mine != null && mine.getOwner().equals(uuid); }
    
    @Override
    public boolean isBlockFromPlayerMine(Location location, Player player) { return isBlockFromPlayerMine(location, player.getUniqueId()); }
    
    @Override
    public boolean canAccessMine(Player player, Mine mine) { return mine != null && mine.canPlayerAccess(player.getUniqueId()); }

    // Nouvelles méthodes pour l'enchantement Nuke
    /**
     * Récupère tous les blocs présents dans la mine
     * @param mine La mine pour laquelle récupérer les blocs
     * @return Une map contenant les blocs (Material) et leur position (Location)
     */
    @Override
    public Map<Location, Material> getMineBlocks(Mine mine) {
        if (!isMineValid(mine)) {
            return new HashMap<>();
        }
        
        Map<Location, Material> blocks = new HashMap<>();
        World world = mine.getLocation().getWorld();
        
        iterateMineBlocks(mine, world, (block, material) -> {
            if (isBreakableBlock(material)) {
                blocks.put(block.getLocation(), material);
            }
        });
        
        return blocks;
    }
    
    /**
     * Calcule le pourcentage de remplissage actuel de la mine
     * @param mine La mine pour laquelle calculer le remplissage
     * @return Le pourcentage de remplissage (0-100)
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public int getBreakableBlockCount(Mine mine) {
        if (!isMineValid(mine)) {
            return 0;
        }
        
        MutableInt count = new MutableInt(0);
        World world = mine.getLocation().getWorld();
        
        iterateMineBlocks(mine, world, (block, material) -> {
            if (isBreakableBlock(material)) {
                count.increment();
            }
        });
        
        return count.getValue();
    }

    private boolean isMineValid(Mine mine) {
        return mine != null && mine.hasMineArea() && mine.getLocation().getWorld() != null;
    }
    
    private boolean isBreakableBlock(Material material) {
        return !material.isAir() && !material.equals(Material.BEDROCK);
    }
    
    private void iterateMineBlocks(Mine mine, World world, BiConsumer<Block, Material> action) {
        for (int x = mine.getMinX(); x <= mine.getMaxX(); x++) {
            for (int y = mine.getMinY(); y <= mine.getMaxY(); y++) {
                for (int z = mine.getMinZ(); z <= mine.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    action.accept(block, block.getType());
                }
            }
        }
    }
    
    // Simple mutable integer helper class
    private static class MutableInt {
        private int value;
        
        public MutableInt(int value) {
            this.value = value;
        }
        
        public void increment() {
            value++;
        }
        
        public int getValue() {
            return value;
        }
    }

    // Accès et permissions
    @Override
    public MineAccess getMineAccess(Player player) { Mine mine = getMine(player); return mine != null ? mine.getMineAccess() : null; }
    
    @Override
    public List<Mine> getTopMines() { return plugin.getStatsManager().getTopMines(); }
    
    @Override
    public void saveStats() { plugin.getStatsManager().saveStats(); }
    
    @Override
    public void syncMineStats(Mine mine) { plugin.getStatsManager().syncMineStats(mine); }

    // Hologrammes
    @Override
    public void createOrUpdateHologram(Mine mine) { if (plugin.getHologramManager() != null) plugin.getHologramManager().createOrUpdateHologram(mine); }
    
    @Override
    public void removeHologram(UUID ownerId) { if (plugin.getHologramManager() != null) plugin.getHologramManager().removeHologram(ownerId); }
    
    @Override
    public void updateAllHolograms() { if (plugin.getHologramManager() != null) plugin.getHologramManager().updateAllHolograms(); }

    // Monde des mines
    @Override
    public World getMineWorld() { return plugin.getMineWorldManager().getMineWorld(); }
    
    @Override
    public String getMineWorldName() { return plugin.getMineWorldManager().getMineWorldName(); }
    
    @Override
    public Location getNextMineLocation() { return plugin.getMineWorldManager().getNextMineLocation(); }
    
    @Override
    public void unloadMineWorld() { plugin.getMineWorldManager().unloadWorld(); }

    // GUIs
    @Override
    public void openMainGUI(Player player) { fr.ju.privateMines.guis.MineMainGUI.openGUI(player); }
    
    @Override
    public void openStatsGUI(Player player) { fr.ju.privateMines.guis.MineStatsGUI.openGUI(player); }
    
    @Override
    public void openVisitorsGUI(Player player, int page) { fr.ju.privateMines.guis.MineVisitorsGUI.openGUI(player, page); }
    
    @Override
    public void openSettingsGUI(Player player) { fr.ju.privateMines.guis.MineSettingsGUI.openGUI(player); }
    
    @Override
    public void openExpandGUI(Player player) { fr.ju.privateMines.guis.MineExpandGUI.openGUI(player); }
    
    @Override
    public void openCompositionGUI(Player player) { fr.ju.privateMines.guis.MineCompositionGUI.openGUI(player); }

    // Protection WorldGuard
    @Override
    public void protectMine(Mine mine, com.sk89q.worldedit.math.BlockVector3[] bounds) { plugin.getMineManager().getMineProtectionManager().protectMine(mine, bounds); }
    
    @Override
    public void unprotectMine(Mine mine) { plugin.getMineManager().getMineProtectionManager().unprotectMine(mine); }
    
    @Override
    public void updateMineProtection(Mine mine) { plugin.getMineManager().getMineProtectionManager().updateMineProtection(mine); }

    // Types et tiers
    /**
     * Récupère la liste complète des paliers (tiers) de mines disponibles
     * @return Une map associant les numéros de palier avec leurs configurations de blocs
     */
    @Override
    public Map<Integer, Map<Material, Double>> getMineTiers() { return plugin.getMineManager().getMineTiers(); }
    
    @Override
    public void reloadMineTiers() { plugin.getMineManager().loadMineTiers(); }

    // Sauvegarde/chargement
    @Override
    public void saveMineData(Player player) { plugin.getMineManager().saveMineData(player); }
    
    @Override
    public void saveMine(Mine mine) { plugin.getMineManager().saveMine(mine); }
    
    @Override
    public void loadMineData() { plugin.getMineManager().loadMineData(); }
    
    @Override
    public void saveAllMineData() { plugin.getMineManager().saveAllMineData(); }
    
    @Override
    public void reloadPlugin() { plugin.reloadPlugin(); }

    /**
     * Définit le palier (tier) de la mine d'un joueur et applique le reset.
     * @param player Le joueur cible
     * @param tier Le nouveau palier
     * @return true si la modification a réussi
     */
    @Override
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
    @Override
    public boolean setMineTier(UUID uuid, int tier) {
        Mine mine = getMine(uuid);
        if (mine == null) return false;
        mine.setTier(tier);
        saveMine(mine);
        resetMine(uuid);
        return true;
    }
} 