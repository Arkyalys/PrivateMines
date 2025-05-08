package fr.ju.privateMines.security;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;

/**
 * Implémentation de IPermissionService utilisant l'API Bukkit.
 */
public class BukkitPermissionService implements IPermissionService {
    
    private final PrivateMines plugin;
    
    public BukkitPermissionService(PrivateMines plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean hasPermission(UUID playerUUID, String permission) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            // Joueur hors ligne, fallback sur une approche de permission par défaut
            return false;
        }
        return player.hasPermission(permission);
    }
    
    @Override
    public boolean isAdmin(UUID playerUUID) {
        return hasPermission(playerUUID, "privateMines.admin");
    }
    
    @Override
    public boolean canPerformMineAction(UUID playerUUID, UUID mineOwnerUUID, String action) {
        // Si c'est le propriétaire de la mine, il peut faire n'importe quelle action
        if (playerUUID.equals(mineOwnerUUID)) {
            return true;
        }
        
        // Les administrateurs ont des droits étendus
        if (isAdmin(playerUUID)) {
            return true;
        }
        
        // Vérification des permissions spécifiques à l'action
        String specificPermission = "privateMines." + action + ".others";
        return hasPermission(playerUUID, specificPermission);
    }
    
    /**
     * Version synchrone qui utilise directement l'objet Player
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null) {
            return false;
        }
        return player.hasPermission(permission);
    }
    
    /**
     * Version synchrone pour vérifier si un joueur est administrateur
     */
    public boolean isAdmin(Player player) {
        return hasPermission(player, "privateMines.admin");
    }
    
    /**
     * Version synchrone pour vérifier si un joueur peut effectuer une action sur une mine
     */
    public boolean canPerformMineAction(Player player, UUID mineOwnerUUID, String action) {
        if (player == null) {
            return false;
        }
        
        // Si c'est le propriétaire de la mine, il peut faire n'importe quelle action
        if (player.getUniqueId().equals(mineOwnerUUID)) {
            return true;
        }
        
        // Les administrateurs ont des droits étendus
        if (isAdmin(player)) {
            return true;
        }
        
        // Vérification des permissions spécifiques à l'action
        String specificPermission = "privateMines." + action + ".others";
        return hasPermission(player, specificPermission);
    }
} 