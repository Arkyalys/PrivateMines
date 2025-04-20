package fr.ju.privateMines.utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.ju.privateMines.PrivateMines;
public class GUIManager {
    private final Map<UUID, String> openInventories;
    public GUIManager(PrivateMines plugin) {
        this.openInventories = new HashMap<>();
    }
    public void registerOpenInventory(Player player, String inventoryType) {
        openInventories.put(player.getUniqueId(), inventoryType);
    }
    public String getOpenInventoryType(Player player) {
        return openInventories.get(player.getUniqueId());
    }
    public void unregisterOpenInventory(Player player) {
        openInventories.remove(player.getUniqueId());
    }
    public ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(ColorUtil.translateColors(name)));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtil.translateColors(line));
            }
            java.util.List<net.kyori.adventure.text.Component> adventureLore = new java.util.ArrayList<>();
            for (String line : coloredLore) {
                adventureLore.add(net.kyori.adventure.text.Component.text(line));
            }
            meta.lore(adventureLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    public ItemStack createGuiItem(Material material, String name, String... loreLines) {
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        return createGuiItem(material, name, lore);
    }
    public void fillEmptySlots(Inventory inventory) {
        ItemStack emptyItem = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, emptyItem);
            }
        }
    }
    public static String formatMaterialName(String materialName) {
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
    public static String createProgressBar(int percentage) {
        int barLength = 20;
        int filledBars = (int) Math.round(percentage / 100.0 * barLength);
        StringBuilder barBuilder = new StringBuilder("&a");
        for (int i = 0; i < filledBars; i++) {
            barBuilder.append("█");
        }
        if (filledBars < barLength) {
            barBuilder.append("&7");
            for (int i = filledBars; i < barLength; i++) {
                barBuilder.append("█");
            }
        }
        barBuilder.append(" &f").append(percentage).append("%");
        return ColorUtil.translateColors(barBuilder.toString());
    }
} 