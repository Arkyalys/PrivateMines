package fr.ju.privateMines.security;

import java.util.UUID;

/**
 * Interface pour la vérification des permissions.
 * Permet de découpler les contrôles d'accès de l'API Bukkit.
 */
public interface IPermissionService {
    
    /**
     * Vérifie si un joueur a une permission spécifique
     * @param playerUUID L'UUID du joueur
     * @param permission La permission à vérifier
     * @return true si le joueur a la permission, false sinon
     */
    boolean hasPermission(UUID playerUUID, String permission);
    
    /**
     * Vérifie si un joueur a un rôle d'administrateur
     * @param playerUUID L'UUID du joueur
     * @return true si le joueur est administrateur, false sinon
     */
    boolean isAdmin(UUID playerUUID);
    
    /**
     * Vérifie si un joueur a la permission d'effectuer une action sur une mine
     * @param playerUUID L'UUID du joueur
     * @param mineOwnerUUID L'UUID du propriétaire de la mine
     * @param action L'action à effectuer (par exemple "reset", "expand", "delete")
     * @return true si le joueur a la permission, false sinon
     */
    boolean canPerformMineAction(UUID playerUUID, UUID mineOwnerUUID, String action);
} 