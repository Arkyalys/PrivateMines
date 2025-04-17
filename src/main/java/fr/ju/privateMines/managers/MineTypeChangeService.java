package fr.ju.privateMines.managers;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
public class MineTypeChangeService {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    public MineTypeChangeService(PrivateMines plugin, MineManager mineManager) {
        this.plugin = plugin;
        this.mineManager = mineManager;
    }
    public boolean setMineType(Player player, String type) {
        Map<String, Map<Material, Double>> mineTypes = mineManager.getMineTypes();
        if (!mineManager.hasMine(player)) {
            player.sendMessage(ColorUtil.deserialize("&cVous n'avez pas de mine à modifier."));
            return false;
        }
        if (!mineTypes.containsKey(type)) {
            player.sendMessage(ColorUtil.deserialize("&cType de mine inconnu: &e" + type));
            return false;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.deserialize("&cErreur lors de la récupération de votre mine."));
            return false;
        }
        mine.setType(type);
        mine.setBlocks(mineTypes.get(type));
        mineManager.resetMine(player);
        mineManager.saveMineData(player);
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createOrUpdateHologram(mine);
        }
        player.sendMessage(ColorUtil.deserialize("&aLe type de votre mine a été changé en: &e" + type));
        return true;
    }
} 