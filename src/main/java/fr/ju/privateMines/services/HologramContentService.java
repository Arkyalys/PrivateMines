package fr.ju.privateMines.services;
import java.util.List;
import java.util.Map;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.HologramManager;
import fr.ju.privateMines.models.Mine;
public class HologramContentService {
    private final PrivateMines plugin;
    private final HologramManager hologramManager;
    public HologramContentService(PrivateMines plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }
    public List<String> generateCentralHologramLines(Mine mine) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public List<String> generateResourceHologramLines(Mine mine) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public List<String> generateAutoMinerHologramLines(Mine mine) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public List<String> generatePlayerInfoHologramLines(Mine mine) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public String createProgressBar(int percentage) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public Map<org.bukkit.Material, Double> getMineResources(Mine mine) {
        throw new UnsupportedOperationException("À implémenter");
    }
    public String formatMaterialName(org.bukkit.Material material) {
        throw new UnsupportedOperationException("À implémenter");
    }
} 