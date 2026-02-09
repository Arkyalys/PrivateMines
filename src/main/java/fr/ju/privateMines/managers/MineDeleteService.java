package fr.ju.privateMines.managers;
import java.util.UUID;

import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class MineDeleteService {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    public MineDeleteService(PrivateMines plugin, MineManager mineManager) {
        this.plugin = plugin;
        this.mineManager = mineManager;
    }
    public boolean deleteMine(Player player) {
        if (!mineManager.hasMine(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-mine"));
            return false;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-mine"));
            return false;
        }
        UUID ownerId = player.getUniqueId();
        mineManager.getMineProtectionManager().unprotectMine(mine);
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().removeHologram(ownerId);
        }
        mineManager.removeMine(ownerId);
        plugin.getMetricsService().incrementMinesDeleted();
        String path = "mines." + ownerId.toString();
        plugin.getConfigManager().getData().set(path, null);
        plugin.getConfigManager().saveData();
        player.sendMessage(plugin.getConfigManager().getMessage("mine-deleted"));
        return true;
    }
} 