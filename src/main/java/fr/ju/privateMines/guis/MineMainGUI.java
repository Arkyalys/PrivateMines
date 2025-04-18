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
public class MineMainGUI {
    private static final String GUI_TITLE = "&8■ &bGestion de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_main";
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        int size = 45; 
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.translateColors(GUI_TITLE));
        boolean hasMine = plugin.getMineManager().hasMine(player);
        if (hasMine) {
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine == null) {
                player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
                return;
            }
            MineStats stats = mine.getStats();
            List<String> infoLore = new ArrayList<>();
            infoLore.add("&7Type: &b" + mine.getType());
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
            ItemStack infoItem = guiManager.createGuiItem(Material.FILLED_MAP, "&e⚒ &bInformations de la Mine", infoLore);
            inventory.setItem(4, infoItem);
            ItemStack teleportItem = guiManager.createGuiItem(Material.ENDER_PEARL, "&e✦ &bTéléporter", 
                    "&7Téléportation vers votre mine",
                    "",
                    "&eCliquez pour vous téléporter");
            inventory.setItem(20, teleportItem);
            ItemStack resetItem = guiManager.createGuiItem(Material.REDSTONE, "&e♻ &bReset", 
                    "&7Réinitialiser les blocs de votre mine",
                    "",
                    "&eCliquez pour réinitialiser");
            inventory.setItem(21, resetItem);
            Material toggleMaterial = mine.isOpen() ? Material.IRON_DOOR : Material.OAK_DOOR;
            String toggleName = mine.isOpen() ? "&e✘ &cFermer la mine" : "&e✓ &aOuvrir la mine";
            String toggleAction = mine.isOpen() ? "fermer" : "ouvrir";
            ItemStack toggleItem = guiManager.createGuiItem(toggleMaterial, toggleName, 
                    "&7" + (mine.isOpen() ? "Empêcher" : "Autoriser") + " les autres joueurs à visiter",
                    "",
                    "&eCliquez pour " + toggleAction);
            inventory.setItem(22, toggleItem);
            ItemStack settingsItem = guiManager.createGuiItem(Material.COMPARATOR, "&e⚙ &bParamètres", 
                    "&7Configurer les options de votre mine",
                    "",
                    "&eCliquez pour ouvrir les paramètres");
            inventory.setItem(23, settingsItem);
            ItemStack contributorsItem = guiManager.createGuiItem(
                Material.PLAYER_HEAD,
                "&e👥 &bContributeurs",
                "&7Voir et gérer les contributeurs de votre mine",
                "",
                "&eCliquez pour gérer les contributeurs"
            );
            inventory.setItem(24, contributorsItem);
            ItemStack upgradeItem;
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Gameplay.upgrades-enabled", true)) {
                upgradeItem = guiManager.createGuiItem(Material.EXPERIENCE_BOTTLE, "&e⬆ &bAméliorer", 
                        "&7Améliorer votre mine au tier suivant",
                        "&7Coût: &b" + getUpgradeCost(mine.getTier()) + " niveaux",
                        "",
                        "&eCliquez pour améliorer");
            } else {
                upgradeItem = guiManager.createGuiItem(Material.BARRIER, "&c⬆ &7Amélioration désactivée", 
                        "&7Les améliorations de mine sont désactivées",
                        "&7sur ce serveur");
            }
            inventory.setItem(31, upgradeItem);
            ItemStack expandItem;
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Gameplay.expansions-enabled", true)) {
                expandItem = guiManager.createGuiItem(Material.GOLDEN_PICKAXE, "&e⇔ &bAgrandir", 
                        "&7Augmenter la taille de votre mine",
                        "&7Taille actuelle: &b" + mine.getSize(),
                        "",
                        "&7Choisissez parmi plusieurs options",
                        "&7d'agrandissement selon vos besoins.",
                        "",
                        "&eCliquez pour voir les options");
            } else {
                expandItem = guiManager.createGuiItem(Material.BARRIER, "&c⇔ &7Agrandissement désactivé", 
                        "&7Les agrandissements de mine sont désactivés",
                        "&7sur ce serveur");
            }
            inventory.setItem(30, expandItem);
            ItemStack deleteItem = guiManager.createGuiItem(Material.TNT, "&c❌ &4Supprimer", 
                    "&7Supprimer définitivement votre mine",
                    "",
                    "&cAttention: Cette action est irréversible!",
                    "",
                    "&eCliquez pour supprimer");
            inventory.setItem(32, deleteItem);
        } else {
            ItemStack infoItem = guiManager.createGuiItem(Material.PAPER, "&e⚠ &bPas de Mine", 
                    "&7Vous n'avez pas encore de mine privée",
                    "&7Créez-en une pour commencer à miner!");
            inventory.setItem(4, infoItem);
            ItemStack createDefaultItem = guiManager.createGuiItem(Material.IRON_PICKAXE, "&e+ &bCréer une Mine Standard", 
                    "&7Créer une mine privée avec la configuration par défaut",
                    "",
                    "&eCliquez pour créer");
            inventory.setItem(22, createDefaultItem);
            if (player.hasPermission("privateMines.admin.create")) {
                ItemStack createCustomItem = guiManager.createGuiItem(Material.DIAMOND_PICKAXE, "&e+ &bCréer une Mine Personnalisée", 
                        "&7Choisir un type de mine personnalisé",
                        "",
                        "&eCliquez pour sélectionner un type");
                inventory.setItem(31, createCustomItem);
            }
        }
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE);
    }
    private static int getUpgradeCost(int currentTier) {
        PrivateMines plugin = PrivateMines.getInstance();
        int baseCost = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.upgrade-cost", 30);
        int multiplier = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.upgrade-cost-multiplier", 2);
        return baseCost * (int)Math.pow(multiplier, currentTier - 1);
    }
    private static int getExpandCost(int currentSize) {
        PrivateMines plugin = PrivateMines.getInstance();
        int baseCost = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.expand-cost", 15);
        int multiplier = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.expand-cost-multiplier", 2);
        return baseCost * (int)Math.pow(multiplier, currentSize - 1);
    }
} 