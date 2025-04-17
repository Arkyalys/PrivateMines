package fr.ju.privateMines.services;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineProtectionManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.MineAreaDetector;

public class MineAreaService {
    private final PrivateMines plugin;
    private final MineAreaDetector areaDetector;
    private final MineProtectionManager protectionManager;

    public MineAreaService(PrivateMines plugin, MineAreaDetector areaDetector, MineProtectionManager protectionManager) {
        this.plugin = plugin;
        this.areaDetector = areaDetector;
        this.protectionManager = protectionManager;
    }

    public boolean setupMineArea(Mine mine) {
        boolean detected = areaDetector.detectMineArea(mine);
        if (!detected) {
            plugin.getLogger().warning("Impossible de détecter la zone de minage pour la mine: " + mine.getOwner());
            return false;
        }
        if (mine.hasMineArea()) {
            protectionManager.protectMine(mine, null);
        }
        return true;
    }
} 