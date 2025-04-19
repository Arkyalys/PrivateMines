package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import net.kyori.adventure.text.Component;
public class MineSettingsGUI {
    private static final String GUI_TITLE = "&8■ &bParamètres de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_settings";
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        if (!plugin.getMineManager().hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
            return;
        }
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text(ColorUtil.translateColors(GUI_TITLE))); 
        List<String> typeLore = new ArrayList<>();
        typeLore.add("&7Type actuel: &b" + mine.getType());
        typeLore.add("");
        typeLore.add("&7Changer le type de votre mine");
        typeLore.add("&7affectera les blocs générés lors");
        typeLore.add("&7du prochain reset.");
        typeLore.add("");
        typeLore.add("&eCliquez pour changer de type");
        ItemStack typeItem = guiManager.createGuiItem(Material.STONE, "&e🧱 &bType de Mine", typeLore);
        inventory.setItem(10, typeItem);
        int currentTax = mine.getTax();
        int maxTax = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.max-tax", 100);
        List<String> taxLore = new ArrayList<>();
        taxLore.add("&7Taxe actuelle: &b" + currentTax + "%");
        taxLore.add("");
        taxLore.add("&7La taxe est prélevée sur les");
        taxLore.add("&7gains des joueurs qui minent");
        taxLore.add("&7dans votre mine.");
        taxLore.add("");
        taxLore.add("&7Taxe maximale: &b" + maxTax + "%");
        taxLore.add("");
        taxLore.add("&eCliquez pour modifier la taxe");
        Material taxMaterial = currentTax > 0 ? Material.GOLD_INGOT : Material.IRON_INGOT;
        ItemStack taxItem = guiManager.createGuiItem(taxMaterial, "&e💰 &bTaxe de la Mine", taxLore);
        inventory.setItem(12, taxItem);
        boolean autoReset = plugin.getConfigManager().getConfig().getBoolean("Config.Mines.auto-reset", true);
        int resetPercentage = plugin.getConfigManager().getConfig().getInt("Config.Mines.auto-reset-percentage", 75);
        List<String> resetLore = new ArrayList<>();
        resetLore.add("&7Reset automatique: " + (autoReset ? "&aActivé" : "&cDésactivé"));
        if (autoReset) {
            resetLore.add("&7Seuil: &b" + resetPercentage + "%");
            resetLore.add("");
            resetLore.add("&7La mine sera automatiquement");
            resetLore.add("&7réinitialisée lorsque " + resetPercentage + "% des");
            resetLore.add("&7blocs auront été minés.");
        } else {
            resetLore.add("");
            resetLore.add("&7Le reset automatique est désactivé");
            resetLore.add("&7dans la configuration du serveur.");
        }
        Material resetMaterial = autoReset ? Material.REDSTONE_LAMP : Material.REDSTONE_TORCH;
        ItemStack resetItem = guiManager.createGuiItem(resetMaterial, "&e♻ &bReset Automatique", resetLore);
        inventory.setItem(14, resetItem);
        List<String> compositionLore = new ArrayList<>();
        compositionLore.add("&7Composition actuelle:");
        Map<Material, Double> blocks = mine.getBlocks();
        blocks.entrySet().stream()
            .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
            .limit(8) 
            .forEach(entry -> {
                String blockName = formatMaterialName(entry.getKey().name());
                double percentage = entry.getValue();
                compositionLore.add("&7- &b" + blockName + "&7: &b" + String.format("%.1f", percentage) + "%");
            });
        if (blocks.size() > 8) {
            compositionLore.add("&7- &b..." + (blocks.size() - 8) + " autres types");
        }
        compositionLore.add("");
        compositionLore.add("&7Ces pourcentages sont appliqués");
        compositionLore.add("&7lors du reset de la mine.");
        ItemStack compositionItem = guiManager.createGuiItem(Material.CHEST, "&e📊 &bComposition", compositionLore);
        inventory.setItem(16, compositionItem);
        boolean protectionEnabled = plugin.getConfigManager().getConfig().getBoolean("Config.Protection.enabled", true);
        List<String> worldLore = new ArrayList<>();
        worldLore.add("&7Protection WorldGuard: " + (protectionEnabled ? "&aActivée" : "&cDésactivée"));
        worldLore.add("");
        if (protectionEnabled) {
            worldLore.add("&7Votre mine est protégée par");
            worldLore.add("&7une région WorldGuard.");
            worldLore.add("");
            worldLore.add("&7Seuls vous et vos invités");
            worldLore.add("&7peuvent y construire.");
        } else {
            worldLore.add("&7La protection des mines est");
            worldLore.add("&7désactivée dans la configuration.");
        }
        Material worldMaterial = protectionEnabled ? Material.SHIELD : Material.BARRIER;
        ItemStack worldItem = guiManager.createGuiItem(worldMaterial, "&e🛡 &bProtection", worldLore);
        inventory.setItem(22, worldItem);
        ItemStack backButton = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour",
                "&7Retourner au menu principal");
        inventory.setItem(31, backButton);
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE);
    }
    private static String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                       .append(part.substring(1))
                       .append(" ");
            }
        }
        return builder.toString().trim();
    }
} 