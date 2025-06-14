package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import net.kyori.adventure.text.Component;
public class MineMainGUI {
    private static final String GUI_TITLE = "&8■ &bGestion de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_main";
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        int size = 45;
        Inventory inventory = Bukkit.createInventory(null, size, Component.text(ColorUtil.translateColors(GUI_TITLE)));
        boolean hasMine = plugin.getMineManager().hasMine(player);
        if (hasMine) {
            handleHasMine(plugin, guiManager, player, inventory);
        } else {
            handleNoMine(plugin, guiManager, player, inventory);
        }
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE);
    }

    private static void handleHasMine(PrivateMines plugin, GUIManager guiManager, Player player, Inventory inventory) {
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("gui.mine-error", "&cErreur lors de la récupération de votre mine."));
            return;
        }
        MineStats stats = mine.getStats();
        inventory.setItem(4, createInfoItem(guiManager, mine, stats));
        inventory.setItem(20, createTeleportItem(guiManager));
        inventory.setItem(21, createResetItem(guiManager));
        inventory.setItem(22, createToggleItem(guiManager, mine));
        inventory.setItem(23, createSettingsItem(guiManager));
        inventory.setItem(24, createContributorsItem(guiManager));
        inventory.setItem(31, createUpgradeItem(plugin, guiManager, mine));
        inventory.setItem(30, createExpandItem(plugin, guiManager, mine));
        inventory.setItem(32, createDeleteItem(guiManager));
    }

    private static void handleNoMine(PrivateMines plugin, GUIManager guiManager, Player player, Inventory inventory) {
        inventory.setItem(4, guiManager.createGuiItem(
                Material.PAPER,
                plugin.getConfigManager().getMessageOrDefault("gui.no-mine-item.name", "&e⚠ &bPas de Mine"),
                plugin.getConfigManager().getMessageOrDefault("gui.no-mine-item.lore1", "&7Vous n'avez pas encore de mine privée"),
                plugin.getConfigManager().getMessageOrDefault("gui.no-mine-item.lore2", "&7Créez-en une pour commencer à miner!")
        ));
        inventory.setItem(22, guiManager.createGuiItem(
                Material.IRON_PICKAXE,
                plugin.getConfigManager().getMessageOrDefault("gui.create-standard.name", "&e+ &bCréer une Mine Standard"),
                plugin.getConfigManager().getMessageOrDefault("gui.create-standard.lore1", "&7Créer une mine privée avec la configuration par défaut"),
                "",
                plugin.getConfigManager().getMessageOrDefault("gui.create-standard.lore2", "&eCliquez pour créer")
        ));
        if (player.hasPermission("privateMines.admin.create")) {
            inventory.setItem(31, guiManager.createGuiItem(
                    Material.DIAMOND_PICKAXE,
                    plugin.getConfigManager().getMessageOrDefault("gui.create-custom.name", "&e+ &bCréer une Mine Personnalisée"),
                    plugin.getConfigManager().getMessageOrDefault("gui.create-custom.lore1", "&7Choisir un type de mine personnalisé"),
                    "",
                    plugin.getConfigManager().getMessageOrDefault("gui.create-custom.lore2", "&eCliquez pour sélectionner un type")
            ));
        }
    }

    private static ItemStack createInfoItem(GUIManager guiManager, Mine mine, MineStats stats) {
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Taille: &b" + mine.getSize());
        infoLore.add("&7Tier: &b" + mine.getTier());
        infoLore.add("&7Taxe: &b" + mine.getTax() + "%");
        infoLore.add("&7État: " + (mine.isOpen() ? "&aOuverte" : "&cFermée"));
        infoLore.add("");
        infoLore.add("&7Blocs minés: &b" + stats.getBlocksMined() + "&7/&b" + stats.getTotalBlocks());
        infoLore.add("&7Pourcentage miné: &b" + stats.getPercentageMined() + "%");
        infoLore.add("&7Visites: &b" + stats.getVisits());
        infoLore.add("");
        infoLore.add("&eCliquez pour voir les statistiques détaillées");
        return guiManager.createGuiItem(Material.FILLED_MAP, "&e⚒ &bInformations de la Mine", infoLore);
    }

    private static ItemStack createTeleportItem(GUIManager guiManager) {
        return guiManager.createGuiItem(Material.ENDER_PEARL, "&e✦ &bTéléporter", "&7Téléportation vers votre mine", "", "&eCliquez pour vous téléporter");
    }

    private static ItemStack createResetItem(GUIManager guiManager) {
        return guiManager.createGuiItem(Material.REDSTONE, "&e♻ &bReset", "&7Réinitialiser les blocs de votre mine", "", "&eCliquez pour réinitialiser");
    }

    private static ItemStack createToggleItem(GUIManager guiManager, Mine mine) {
        Material toggleMaterial = mine.isOpen() ? Material.IRON_DOOR : Material.OAK_DOOR;
        String toggleName = mine.isOpen() ? "&e✘ &cFermer la mine" : "&e✓ &aOuvrir la mine";
        String toggleAction = mine.isOpen() ? "fermer" : "ouvrir";
        return guiManager.createGuiItem(toggleMaterial, toggleName, "&7" + (mine.isOpen() ? "Empêcher" : "Autoriser") + " les autres joueurs à visiter", "", "&eCliquez pour " + toggleAction);
    }

    private static ItemStack createSettingsItem(GUIManager guiManager) {
        return guiManager.createGuiItem(Material.COMPARATOR, "&e⚙ &bParamètres", "&7Configurer les options de votre mine", "", "&eCliquez pour ouvrir les paramètres");
    }

    private static ItemStack createContributorsItem(GUIManager guiManager) {
        return guiManager.createGuiItem(Material.PLAYER_HEAD, "&e👥 &bContributeurs", "&7Voir et gérer les contributeurs de votre mine", "", "&eCliquez pour gérer les contributeurs");
    }

    private static ItemStack createUpgradeItem(PrivateMines plugin, GUIManager guiManager, Mine mine) {
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Gameplay.upgrades-enabled", true)) {
            return guiManager.createGuiItem(Material.EXPERIENCE_BOTTLE, "&e⬆ &bAméliorer", "&7Améliorer votre mine au tier suivant", "&7Coût: &b" + getUpgradeCost(mine.getTier()) + " niveaux", "", "&eCliquez pour améliorer");
        } else {
            return guiManager.createGuiItem(Material.BARRIER, "&c⬆ &7Amélioration désactivée", "&7Les améliorations de mine sont désactivées", "&7sur ce serveur");
        }
    }

    private static ItemStack createExpandItem(PrivateMines plugin, GUIManager guiManager, Mine mine) {
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Gameplay.expansions-enabled", true)) {
            return guiManager.createGuiItem(Material.GOLDEN_PICKAXE, "&e⇔ &bAgrandir", "&7Augmenter la taille de votre mine", "&7Taille actuelle: &b" + mine.getSize(), "", "&7Choisissez parmi plusieurs options", "&7d'agrandissement selon vos besoins.", "", "&eCliquez pour voir les options");
        } else {
            return guiManager.createGuiItem(Material.BARRIER, "&c⇔ &7Agrandissement désactivé", "&7Les agrandissements de mine sont désactivés", "&7sur ce serveur");
        }
    }

    private static ItemStack createDeleteItem(GUIManager guiManager) {
        return guiManager.createGuiItem(Material.TNT, "&c❌ &4Supprimer", "&7Supprimer définitivement votre mine", "", "&cAttention: Cette action est irréversible!", "", "&eCliquez pour supprimer");
    }

    private static int getUpgradeCost(int currentTier) {
        PrivateMines plugin = PrivateMines.getInstance();
        int baseCost = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.upgrade-cost", 30);
        int multiplier = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.upgrade-cost-multiplier", 2);
        return baseCost * (int)Math.pow(multiplier, currentTier - 1);
    }
} 