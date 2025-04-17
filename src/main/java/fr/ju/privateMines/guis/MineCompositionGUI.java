package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
public class MineCompositionGUI {
    private static final String GUI_TITLE = "&8■ &bComposition de la Mine &8■";
    private static final String INVENTORY_TYPE = "mine_composition";
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
        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.translateColors(GUI_TITLE)); 
        List<String> titleLore = new ArrayList<>();
        titleLore.add("&7Voici la liste complète des blocs");
        titleLore.add("&7qui composent votre mine et leur");
        titleLore.add("&7pourcentage de distribution.");
        ItemStack titleItem = guiManager.createGuiItem(Material.CHEST, "&eComposition de la Mine", titleLore);
        inventory.setItem(4, titleItem);
        Map<Material, Double> blocks = mine.getBlocks();
        if (blocks.isEmpty()) {
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("&cAucun bloc défini pour cette mine.");
            emptyLore.add("&7Contactez un administrateur");
            emptyLore.add("&7pour configurer votre mine.");
            ItemStack emptyItem = guiManager.createGuiItem(Material.BARRIER, "&cMine non configurée", emptyLore);
            inventory.setItem(22, emptyItem);
        } else {
            List<Map.Entry<Material, Double>> sortedBlocks = blocks.entrySet().stream()
                .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
            int slot = 19; 
            for (Map.Entry<Material, Double> entry : sortedBlocks) {
                if (slot > 44 || (slot % 9 == 8)) {
                    if (slot % 9 == 8) slot += 2;
                    else break;
                }
                Material material = entry.getKey();
                double percentage = entry.getValue();
                List<String> blockLore = new ArrayList<>();
                blockLore.add("&7Pourcentage: &b" + String.format("%.1f", percentage) + "%");
                blockLore.add("&7Type: &b" + formatMaterialName(material.name()));
                if (material.isBlock()) {
                    blockLore.add("&7Catégorie: &bBloc solide");
                } else if (material.name().contains("ORE")) {
                    blockLore.add("&7Catégorie: &bMinerai");
                } else {
                    blockLore.add("&7Catégorie: &bAutre");
                }
                ItemStack blockItem = new ItemStack(material);
                ItemMeta meta = blockItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtil.translateColors("&b" + formatMaterialName(material.name())));
                    List<String> translatedLore = blockLore.stream()
                            .map(ColorUtil::translateColors)
                            .collect(Collectors.toList());
                    meta.setLore(translatedLore);
                    blockItem.setItemMeta(meta);
                }
                inventory.setItem(slot, blockItem);
                slot++;
            }
        }
        List<String> graphLore = new ArrayList<>();
        graphLore.add("&7Visualisez la composition de votre mine");
        graphLore.add("&7sous forme de graphique coloré");
        ItemStack graphItem = guiManager.createGuiItem(Material.MAP, "&eVisualiser le graphique", graphLore);
        inventory.setItem(48, graphItem);
        ItemStack backButton = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour",
                "&7Retourner au menu des statistiques");
        inventory.setItem(50, backButton);
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