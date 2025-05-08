package fr.ju.privateMines.services;

import java.util.UUID;

import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;

/**
 * Adaptateur qui implémente IStatsService en utilisant le StatsManager existant.
 * Permet de découpler les modèles de données du StatsManager.
 */
public class StatsServiceAdapter implements IStatsService {
    
    private final StatsManager statsManager;
    
    public StatsServiceAdapter(StatsManager statsManager) {
        this.statsManager = statsManager;
    }
    
    @Override
    public boolean incrementBlocksMined(Mine mine) {
        if (statsManager == null) {
            // Fallback en cas d'absence de StatsManager
            mine.getStats().incrementBlocksMined();
            return false;
        }
        return statsManager.incrementBlocksMined(mine);
    }
    
    @Override
    public MineStats getStats(UUID owner) {
        if (statsManager == null) {
            return null;
        }
        return statsManager.getStats(owner);
    }
    
    @Override
    public void syncMineStats(Mine mine) {
        if (statsManager != null) {
            statsManager.syncMineStats(mine);
        }
    }
    
    @Override
    public void onMineReset(Mine mine) {
        if (statsManager != null) {
            statsManager.onMineReset(mine);
        }
    }
    
    @Override
    public boolean isEnabled() {
        return statsManager != null && statsManager.isEnabled();
    }
} 