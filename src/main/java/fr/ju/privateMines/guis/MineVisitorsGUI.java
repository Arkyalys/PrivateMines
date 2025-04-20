package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import net.kyori.adventure.text.Component;

public class MineVisitorsGUI {
    private static final String GUI_TITLE = "&8■ &bContributeurs de la Mine &8■";
    private static final String INVENTORY_TYPE = "mine_contributors";
    private static final String ACTION_TYPE = "mine_contributor_action";
    public static final int PAGE_SIZE = 36;

    public static void openGUI(Player player, int page) {
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
        List<UUID> contributors = new ArrayList<>(mine.getContributors());
        int totalContributors = contributors.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalContributors / PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(ColorUtil.translateColors(GUI_TITLE)));
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Nombre de contributeurs: &b" + totalContributors);
        infoLore.add("");
        infoLore.add("&7Page: &b" + (page + 1) + "&7/&b" + totalPages);
        ItemStack infoItem = guiManager.createGuiItem(Material.BOOK, "&e👥 &bContributeurs", infoLore);
        inventory.setItem(4, infoItem);
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalContributors);
        for (int i = startIndex; i < endIndex; i++) {
            UUID contributorId = contributors.get(i);
            int slot = 9 + (i - startIndex);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(contributorId);
            String name = offlinePlayer.getName();
            if (name == null) name = contributorId.toString().substring(0, 8);
            ItemStack contributorItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) contributorItem.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(ColorUtil.translateColors("&b" + name)));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.translateColors("&7Contributeur de la mine"));
                lore.add("");
                lore.add(ColorUtil.translateColors("&cCliquez pour retirer ce contributeur"));
                List<Component> loreComponents = lore.stream()
                    .map(line -> Component.text(line))
                    .collect(Collectors.toList());
                meta.lore(loreComponents);
                meta.setOwningPlayer(offlinePlayer);
                contributorItem.setItemMeta(meta);
            }
            inventory.setItem(slot, contributorItem);
        }
        // Bouton pour ajouter un contributeur
        ItemStack addItem = guiManager.createGuiItem(Material.EMERALD, "&aAjouter un contributeur", Arrays.asList("&7Cliquez pour ajouter un joueur comme contributeur.", "&7Un menu s'ouvrira pour entrer le pseudo."));
        inventory.setItem(6, addItem);
        if (page > 0) {
            ItemStack previousItem = guiManager.createGuiItem(Material.ARROW, "&e◀ &bPage précédente", "&7Aller à la page " + page);
            inventory.setItem(45, previousItem);
        }
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour", "&7Retourner au menu principal");
        inventory.setItem(49, backItem);
        if (page < totalPages - 1) {
            ItemStack nextItem = guiManager.createGuiItem(Material.ARROW, "&e▶ &bPage suivante", "&7Aller à la page " + (page + 2));
            inventory.setItem(53, nextItem);
        }
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE + ":" + page);
    }

    public static void openActionGUI(Player player, UUID targetId) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetId);
        String targetName = targetPlayer.getName();
        if (targetName == null) targetName = targetId.toString().substring(0, 8);
        Mine ownerMine = plugin.getMineManager().getMine(player).orElse(null);
        if (ownerMine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(ColorUtil.translateColors("&8■ &bContributeur: " + targetName + " &8■")));
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerInfo.getItemMeta();
        if (playerMeta != null) {
            playerMeta.displayName(Component.text(ColorUtil.translateColors("&b" + targetName)));
            List<String> playerLore = new ArrayList<>();
            playerLore.add(ColorUtil.translateColors("&7Contributeur de la mine"));
            playerLore.add("");
            playerLore.add(ColorUtil.translateColors("&cCliquez pour retirer ce contributeur"));
            List<Component> playerLoreComponents = playerLore.stream()
                .map(line -> Component.text(line))
                .collect(Collectors.toList());
            playerMeta.lore(playerLoreComponents);
            playerMeta.setOwningPlayer(targetPlayer);
            playerInfo.setItemMeta(playerMeta);
        }
        inventory.setItem(13, playerInfo);
        ItemStack removeItem = guiManager.createGuiItem(Material.BARRIER, "&cRetirer ce contributeur", Arrays.asList("&7Cliquez pour retirer ce joueur de la liste des contributeurs."));
        inventory.setItem(15, removeItem);
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour", Arrays.asList("&7Retourner à la liste des contributeurs"));
        inventory.setItem(18, backItem);
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, ACTION_TYPE + ":" + targetId.toString());
    }
} 