package fr.ju.privateMines.services;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.HologramManager;
import fr.ju.privateMines.models.Mine;
public class HologramUpdateService {
    private final PrivateMines plugin;
    private final HologramManager hologramManager;
    public HologramUpdateService(PrivateMines plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }
    public void updateAllHolograms() {
        for (Mine mine : plugin.getMineManager().getAllMines()) {
            hologramManager.createOrUpdateHologram(mine);
        }
    }
} 