package fr.ju.privateMines.services;

import java.util.UUID;

import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;

/**
 * Interface de service pour la gestion des statistiques des mines.
 * Permet de découpler les modèles de données des services de traitement.
 */
public interface IStatsService {
    
    /**
     * Incrémente le compteur de blocs minés pour une mine
     * @param mine La mine concernée
     * @return true si la mine devrait être réinitialisée (seuil atteint), false sinon
     */
    boolean incrementBlocksMined(Mine mine);
    
    /**
     * Récupère les statistiques d'une mine spécifique
     * @param owner UUID du propriétaire de la mine
     * @return Les statistiques associées ou null si non trouvées
     */
    MineStats getStats(UUID owner);
    
    /**
     * Synchronise les statistiques entre la mine et le service de statistiques
     * @param mine La mine dont les statistiques doivent être synchronisées
     */
    void syncMineStats(Mine mine);
    
    /**
     * Met à jour les statistiques d'une mine lors d'une réinitialisation
     * @param mine La mine réinitialisée
     */
    void onMineReset(Mine mine);
    
    /**
     * Indique si le service de statistiques est activé
     * @return true si activé, false sinon
     */
    boolean isEnabled();
} 