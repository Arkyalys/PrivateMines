package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import net.kyori.adventure.text.Component;
public class MineTypeGUI {
    private static final String GUI_TITLE = "&8■ &bTypes de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_type";
    public static void openGUI(Player player, boolean isChangingType) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        if (isChangingType && !plugin.getMineManager().hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
            return;
        }
        ConfigurationSection typesSection = plugin.getConfigManager().getConfig()
                .getConfigurationSection("Config.Mines.default.types");
        if (typesSection == null || typesSection.getKeys(false).isEmpty()) {
            player.sendMessage(ColorUtil.translateColors("&cAucun type de mine n'est configuré. Contactez un administrateur."));
            return;
        }
        Set<String> mineTypes = typesSection.getKeys(false);
        int size = ((mineTypes.size() / 9) + 1) * 9 + 18; 
        size = Math.min(54, Math.max(36, size)); 
        Inventory inventory = Bukkit.createInventory(null, size, Component.text(ColorUtil.translateColors(GUI_TITLE)));
        String title = isChangingType ? "&e⚙ &bChangement de Type" : "&e+ &bCréation de Mine";
        String desc = isChangingType ? 
                "&7Choisissez le nouveau type de votre mine" : 
                "&7Sélectionnez un type pour votre nouvelle mine";
        ItemStack infoItem = guiManager.createGuiItem(Material.PAPER, title, desc);
        inventory.setItem(4, infoItem);
        int slot = 9;
        for (String type : mineTypes) {
            if (slot >= size - 9) break; 
            Material displayMaterial = getMaterialForType(typesSection, type);
            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &b" + type);
            lore.add("");
            ConfigurationSection blocksSection = typesSection.getConfigurationSection(type + ".blocks");
            if (blocksSection != null) {
                lore.add("&7Composition:");
                for (String materialName : blocksSection.getKeys(false)) {
                    double chance = blocksSection.getDouble(materialName);
                    String formattedName = formatMaterialName(materialName);
                    lore.add("&7- &b" + formattedName + "&7: &b" + String.format("%.1f", chance) + "%");
                }
            }
            lore.add("");
            lore.add("&eCliquez pour sélectionner");
            ItemStack typeItem = guiManager.createGuiItem(displayMaterial, "&e🧱 &b" + formatMaterialName(type), lore);
            inventory.setItem(slot, typeItem);
            slot++;
        }
        ItemStack backButton = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour",
                "&7Retourner au menu " + (isChangingType ? "des paramètres" : "principal"));
        inventory.setItem(size - 5, backButton);
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        String inventoryTypeWithMeta = isChangingType ? INVENTORY_TYPE + ":change" : INVENTORY_TYPE + ":create";
        guiManager.registerOpenInventory(player, inventoryTypeWithMeta);
    }
    private static Material getMaterialForType(ConfigurationSection typesSection, String type) {
        String displayItem = typesSection.getString(type + ".display-item");
        if (displayItem != null) {
            try {
                Material material = Material.valueOf(displayItem.toUpperCase());
                return material;
            } catch (IllegalArgumentException e) {
            }
        }
        ConfigurationSection blocksSection = typesSection.getConfigurationSection(type + ".blocks");
        if (blocksSection != null) {
            double maxChance = 0;
            Material commonMaterial = Material.STONE; 
            for (String materialName : blocksSection.getKeys(false)) {
                double chance = blocksSection.getDouble(materialName);
                if (chance > maxChance) {
                    try {
                        Material material = Material.valueOf(materialName.toUpperCase());
                        commonMaterial = material;
                        maxChance = chance;
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
            return commonMaterial;
        }
        return Material.STONE;
    }
    private static String formatMaterialName(String name) {
        String[] parts = name.toLowerCase().split("_");
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