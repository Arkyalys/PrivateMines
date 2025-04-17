package fr.ju.privateMines.managers;
import org.bukkit.entity.Player;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
public class MineExpansionService {
    private final PrivateMines plugin;
    private final ConfigManager configManager;
    private final MineProtectionManager protectionManager;
    public MineExpansionService(PrivateMines plugin, MineProtectionManager protectionManager) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.protectionManager = protectionManager;
    }
    public boolean expandMine(Player player, Mine mine, int expandSize) {
        return false; 
    }
} 