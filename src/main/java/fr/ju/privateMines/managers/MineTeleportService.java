package fr.ju.privateMines.managers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;

public class MineTeleportService {
    private final PrivateMines plugin;
    
    public MineTeleportService(PrivateMines plugin) {
        this.plugin = plugin;
    }
    
    public boolean teleportToMine(Player owner, Player visitor, Mine mine) {
        Location teleportLocation = getBetterTeleportLocation(mine);
        return visitor.teleport(teleportLocation);
    }
    
    /**
     * Retourne la meilleure position de téléportation pour une mine.
     */
    public Location getBetterTeleportLocation(Mine mine) {
        debug("Calcul du point de téléportation pour la mine de UUID: " + mine.getOwner());
              
        // Vérifie s'il existe déjà un point de téléportation personnalisé
        if (hasCustomTeleportLocation(mine)) {
            return mine.getTeleportLocation();
        }
        
        // Génère un point de téléportation basé sur la zone de la mine
        if (mine.hasMineArea()) {
            return getMineAreaTeleportLocation(mine);
        }
        
        // Utilise les limites du schéma si disponibles
        if (mine.hasSchematicBounds()) {
            return getSchematicBasedTeleportLocation(mine);
        }
        
        // Fallback: utilise la position de base de la mine
        return getBaseTeleportLocation(mine);
    }
    
    /**
     * Vérifie si la mine a un point de téléportation personnalisé valide
     */
    private boolean hasCustomTeleportLocation(Mine mine) {
        if (mine.getTeleportLocation() != null && mine.getTeleportLocation().getWorld() != null) {
            debug("Utilisation du point de téléportation personnalisé");
            return true;
        }
        return false;
    }
    
    /**
     * Génère un point de téléportation basé sur la zone de la mine
     */
    private Location getMineAreaTeleportLocation(Mine mine) {
        debug("Calcul d'un point de téléportation basé sur les limites de la mine");
        
        World world = mine.getLocation().getWorld();
        if (world == null) {
            plugin.getLogger().warning("Le monde de la mine est null, utilisation de la position de base");
            return getBaseTeleportLocation(mine);
        }
        
        logMineAreaDebugInfo(mine);
        
        // Calcul du point de téléportation initial
        int centerZ = (mine.getMinZ() + mine.getMaxZ()) / 2;
        int teleportX = mine.getMinX() - 2;
        int teleportZ = centerZ;
        int teleportY = Math.max(64, mine.getMinY() + 1);
        
        Location teleportLocation = new Location(world, teleportX + 0.5, teleportY, teleportZ + 0.5);
        teleportLocation.setYaw(90);
        
        debug("Point de téléportation calculé: " + formatLocation(teleportLocation));
        
        // Vérifie si la position est sûre (pas de blocs solides)
        if (!isSafeLocation(teleportLocation)) {
            return findSafeAlternativeLocation(mine, teleportX, teleportZ, teleportY, world);
        }
        
        return teleportLocation;
    }
    
    /**
     * Vérifie si une position est sûre pour la téléportation (espace libre)
     */
    private boolean isSafeLocation(Location location) {
        return location.getBlock().getType() == Material.AIR && 
               location.clone().add(0, 1, 0).getBlock().getType() == Material.AIR;
    }
    
    /**
     * Recherche une position de téléportation alternative sûre
     */
    private Location findSafeAlternativeLocation(Mine mine, int teleportX, int teleportZ, int teleportY, World world) {
        debug("La position calculée n'est pas sûre, recherche d'une position alternative");
        
        // Essaie de trouver un espace libre en montant
        for (int checkY = teleportY; checkY < Math.min(255, teleportY + 20); checkY++) {
            Location check = new Location(world, teleportX + 0.5, checkY, teleportZ + 0.5);
            check.setYaw(90);
            
            if (isSafeLocation(check)) {
                debug("Position sûre trouvée à Y=" + checkY);
                return check;
            }
        }
        
        // Si aucun espace sûr trouvé, retourne à la position de base
        plugin.getLogger().warning("Aucune position sûre trouvée, retour à la position de base");
        return getBaseTeleportLocation(mine);
    }
    
    /**
     * Génère un point de téléportation basé sur les limites du schéma
     */
    private Location getSchematicBasedTeleportLocation(Mine mine) {
        plugin.getLogger().info("Utilisation des limites du schéma pour la téléportation");
        
        double teleportX = mine.getSchematicMinX() - 2;
        double centerY = Math.max(64, mine.getSchematicMinY() + 1);
        double centerZ = (mine.getSchematicMinZ() + mine.getSchematicMaxZ()) / 2;
        
        Location schematicCenter = new Location(mine.getLocation().getWorld(), teleportX, centerY, centerZ);
        schematicCenter.setYaw(90);
        
        plugin.getLogger().info("Point de téléportation basé sur le schéma: " + schematicCenter.toString());
        return schematicCenter;
    }
    
    /**
     * Retourne la position de téléportation de base (fallback)
     */
    private Location getBaseTeleportLocation(Mine mine) {
        plugin.getLogger().info("Aucune zone ou schéma défini, utilisation de la position de base de la mine");
        Location baseLoc = mine.getLocation().clone().add(0.5, 1, 0.5);
        baseLoc.setYaw(90);
        return baseLoc;
    }
    
    /**
     * Affiche les informations de debug sur la zone de la mine
     */
    private void logMineAreaDebugInfo(Mine mine) {
        debug("Position de la mine: " + formatLocation(mine.getLocation()));
        debug("Zone de la mine: (" + mine.getMinX() + "," + mine.getMinY() + "," + mine.getMinZ() + 
              ") à (" + mine.getMaxX() + "," + mine.getMaxY() + "," + mine.getMaxZ() + ")");
    }
    
    /**
     * Formate une Location pour l'affichage dans les logs
     */
    private String formatLocation(Location location) {
        return location.getWorld().getName() + " [" + 
               location.getX() + "," + 
               location.getY() + "," + 
               location.getZ() + "]";
    }
    
    /**
     * Log un message de debug si le mode debug est activé
     */
    private void debug(String message) {
        if (PrivateMines.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
} 