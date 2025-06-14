package fr.ju.privateMines.managers;
import java.time.Duration;

import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
public class MineUpgradeService {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    public MineUpgradeService(PrivateMines plugin, MineManager mineManager) {
        this.plugin = plugin;
        this.mineManager = mineManager;
    }
    public boolean upgradeMine(Player player) {
        return expandMine(player);
    }
    public boolean expandMine(Player player) {
        int defaultExpandSize = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.default-expand-size", 2);
        return expandMine(player, defaultExpandSize);
    }
    public boolean expandMine(Player player, int expandSize) {
        if (!mineManager.hasMine(player)) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("upgrade.no-mine", "&cVous n'avez pas de mine à améliorer."));
            return false;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("gui.mine-error", "&cErreur lors de la récupération de votre mine."));
            return false;
        }
        int currentTier = mine.getTier();
        int nextTier = currentTier + 1;
        
        // Vérifier si le tier suivant existe dans la configuration
        if (!mineManager.getMineTiers().containsKey(nextTier)) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("upgrade.unavailable", "&cLe palier suivant n'est pas disponible."));
            return false;
        }
        
        // Mettre à jour le tier de la mine
        mine.setTier(nextTier);
        plugin.getMetricsService().incrementMineUpgrades();
        
        // Réinitialiser la mine avec les nouveaux blocs du tier
        mineManager.resetMine(player);
        
        // Sauvegarder les données de la mine
        mineManager.saveMineData(player);
        
        // Mettre à jour l'hologramme si nécessaire
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createOrUpdateHologram(mine);
        }
        
        // Notifier le joueur
        player.sendMessage(plugin.getConfigManager().getMessageOrDefault("upgrade.success", "&aVotre mine a été améliorée au palier &e%tier%&a!").replace("%tier%", String.valueOf(nextTier)));
        Title title = Title.title(
            Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessageOrDefault("titles.mine-upgraded.title", "&6&lMine améliorée"))),
            Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessageOrDefault("titles.mine-upgraded.subtitle", "&eNouveau palier: %tier%").replace("%tier%", String.valueOf(nextTier)))),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }
} 