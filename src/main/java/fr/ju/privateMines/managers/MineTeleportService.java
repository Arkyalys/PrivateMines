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

        // Priorité 1: la zone de mine (le plus fiable)
        if (mine.hasMineArea()) {
            return getMineAreaTeleportLocation(mine);
        }

        // Priorité 2: les limites du schéma
        if (mine.hasSchematicBounds()) {
            return getSchematicBasedTeleportLocation(mine);
        }

        // Priorité 3: point de téléportation personnalisé
        if (hasCustomTeleportLocation(mine)) {
            return mine.getTeleportLocation();
        }

        // Fallback: position de base de la mine
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
     * Génère un point de téléportation basé sur la zone de la mine.
     * Téléporte au centre de la mine area, au-dessus de la zone de minage.
     * Ne retourne JAMAIS à la position de base (mine origin), car elle peut être
     * très loin de la zone de minage réelle.
     */
    private Location getMineAreaTeleportLocation(Mine mine) {
        debug("Calcul d'un point de téléportation basé sur les limites de la mine");

        World world = mine.getLocation().getWorld();
        if (world == null) {
            plugin.getLogger().warning("Le monde de la mine est null, utilisation de la position de base");
            return getBaseTeleportLocation(mine);
        }

        logMineAreaDebugInfo(mine);

        // Téléporter au centre de la mine area, juste au-dessus
        int centerX = (mine.getMinX() + mine.getMaxX()) / 2;
        int centerZ = (mine.getMinZ() + mine.getMaxZ()) / 2;
        int teleportY = mine.getMaxY() + 2;

        Location teleportLocation = new Location(world, centerX + 0.5, teleportY, centerZ + 0.5);
        teleportLocation.setYaw(90);

        debug("Point de téléportation calculé: " + formatLocation(teleportLocation));

        // Cherche un espace libre vers le haut si nécessaire
        if (!isSafeLocation(teleportLocation)) {
            for (int checkY = teleportY; checkY < teleportY + 20; checkY++) {
                Location check = new Location(world, centerX + 0.5, checkY, centerZ + 0.5);
                check.setYaw(90);
                if (isSafeLocation(check)) {
                    debug("Position sûre trouvée à Y=" + checkY);
                    return check;
                }
            }
            // Force la TP au centre de la mine area plutôt que retourner à l'origin
            debug("Aucune position sûre trouvée, téléportation forcée au centre de la mine area");
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
     * Génère un point de téléportation basé sur les limites du schéma
     */
    private Location getSchematicBasedTeleportLocation(Mine mine) {
        debug("Utilisation des limites du schéma pour la téléportation");

        double teleportX = mine.getSchematicMinX() - 2;
        double centerY = Math.max(64, mine.getSchematicMinY() + 1);
        double centerZ = (mine.getSchematicMinZ() + mine.getSchematicMaxZ()) / 2;

        Location schematicCenter = new Location(mine.getLocation().getWorld(), teleportX, centerY, centerZ);
        schematicCenter.setYaw(90);

        debug("Point de téléportation basé sur le schéma: " + schematicCenter);
        return schematicCenter;
    }

    /**
     * Retourne la position de téléportation de base (fallback)
     */
    private Location getBaseTeleportLocation(Mine mine) {
        debug("Aucune zone ou schéma défini, utilisation de la position de base de la mine");
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
    
    private void debug(String message) {
        PrivateMines.debugLog("[TELEPORT] " + message);
    }
} 