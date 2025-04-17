package fr.ju.privateMines.managers;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
public class MineTaxService {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    public MineTaxService(PrivateMines plugin, MineManager mineManager) {
        this.plugin = plugin;
        this.mineManager = mineManager;
    }
    public boolean setMineTax(Player player, int tax) {
        if (!mineManager.hasMine(player)) {
            player.sendMessage(ColorUtil.deserialize("&cVous n'avez pas de mine à modifier."));
            return false;
        }
        if (tax < 0 || tax > 100) {
            player.sendMessage(ColorUtil.deserialize("&cLa taxe doit être comprise entre 0 et 100%."));
            return false;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.deserialize("&cErreur lors de la récupération de votre mine."));
            return false;
        }
        mine.setTax(tax);
        mineManager.saveMineData(player);
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createOrUpdateHologram(mine);
        }
        player.sendMessage(ColorUtil.deserialize("&aLa taxe de votre mine a été définie à &e" + tax + "%&a."));
        return true;
    }
} 