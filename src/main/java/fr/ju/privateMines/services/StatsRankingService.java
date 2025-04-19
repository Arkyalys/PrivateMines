package fr.ju.privateMines.services;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.StatsManager;
import fr.ju.privateMines.models.Mine;
public class StatsRankingService {
    private final PrivateMines plugin;
    public StatsRankingService(PrivateMines plugin, StatsManager statsManager) {
        this.plugin = plugin;
    }
    public List<Mine> getTopMines() {
        Collection<Mine> allMines = plugin.getMineManager().getAllMines();
        List<Mine> sortedMines = new ArrayList<>(allMines);
        sortedMines.sort((m1, m2) -> {
            int m1Blocks = m1.getStats().getBlocksMined();
            int m2Blocks = m2.getStats().getBlocksMined();
            return Integer.compare(m2Blocks, m1Blocks);
        });
        return sortedMines;
    }
} 