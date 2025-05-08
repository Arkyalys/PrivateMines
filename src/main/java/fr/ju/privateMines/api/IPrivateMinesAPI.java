package fr.ju.privateMines.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineAccess;

/**
 * Interface publique de l'API PrivateMines
 * Cette interface définit tous les points d'entrée publics du plugin
 * et permet de découpler l'implémentation interne de l'API exposée.
 */
public interface IPrivateMinesAPI {
    
    // Mines - Gestion basique
    boolean hasMine(Player player);
    boolean hasMine(UUID uuid);
    Mine getMine(Player player);
    Mine getMine(UUID uuid);
    Collection<Mine> getAllMines();
    Map<UUID, Mine> getPlayerMines();
    List<Mine> getPublicMines();
    boolean hasMineAt(Location location);
    Mine createMine(Player player);
    boolean deleteMine(Player player);
    boolean deleteMine(UUID uuid);
    boolean resetMine(Player player);
    boolean resetMine(UUID uuid);
    
    // Mines - Gestion avancée
    boolean expandMine(Player player);
    boolean expandMine(Player player, int expandSize);
    boolean upgradeMine(Player player);
    boolean setMineOpen(Player player, boolean isOpen);
    boolean setMineTax(Player player, int tax);
    int getMineTax(Player player);
    int getMineTax(UUID uuid);
    
    // Mines - Localisation et téléportation
    Location getTeleportLocation(Player player);
    boolean setTeleportLocation(Player player, Location location);
    boolean teleportPlayerToMine(Player player, Mine targetMine);
    Mine getMineAtLocation(Location location);
    boolean isLocationInMineArea(Location location, Mine mine);
    boolean isBlockFromPlayerMine(Location location, UUID uuid);
    boolean isBlockFromPlayerMine(Location location, Player player);
    boolean canAccessMine(Player player, Mine mine);
    
    // Mines - Analyse des blocs
    Map<Location, Material> getMineBlocks(Mine mine);
    float getMineFillRatio(Mine mine);
    int getTotalBlockCount(Mine mine);
    Material getMineBlockType(Mine mine);
    int getBreakableBlockCount(Mine mine);
    
    // Accès et permissions
    MineAccess getMineAccess(Player player);
    List<Mine> getTopMines();
    void saveStats();
    void syncMineStats(Mine mine);
    
    // Hologrammes
    void createOrUpdateHologram(Mine mine);
    void removeHologram(UUID ownerId);
    void updateAllHolograms();
    
    // Monde des mines
    World getMineWorld();
    String getMineWorldName();
    Location getNextMineLocation();
    void unloadMineWorld();
    
    // GUIs
    void openMainGUI(Player player);
    void openStatsGUI(Player player);
    void openVisitorsGUI(Player player, int page);
    void openSettingsGUI(Player player);
    void openExpandGUI(Player player);
    void openCompositionGUI(Player player);
    
    // Protection
    void protectMine(Mine mine, com.sk89q.worldedit.math.BlockVector3[] bounds);
    void unprotectMine(Mine mine);
    void updateMineProtection(Mine mine);
    
    // Types et tiers
    Map<Integer, Map<Material, Double>> getMineTiers();
    void reloadMineTiers();
    
    // Sauvegarde/chargement
    void saveMineData(Player player);
    void saveMine(Mine mine);
    void loadMineData();
    void saveAllMineData();
    void reloadPlugin();
    
    // Gestion des tiers
    boolean setMineTier(Player player, int tier);
    boolean setMineTier(UUID uuid, int tier);
} 