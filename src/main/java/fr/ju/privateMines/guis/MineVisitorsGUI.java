package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import fr.ju.privateMines.models.MineAccess;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;

public class MineVisitorsGUI {
    private static final String GUI_TITLE = "&8■ &bVisiteurs de la Mine &8■";
    private static final String INVENTORY_TYPE = "mine_visitors";
    private static final String ACTION_TYPE = "mine_visitor_action";
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
        MineStats stats = mine.getStats();
        Map<UUID, Integer> visitorStats = stats.getVisitorStats();
        MineAccess mineAccess = mine.getMineAccess();
        List<Map.Entry<UUID, Integer>> sortedVisitors = visitorStats.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .filter(entry -> !entry.getKey().equals(mine.getOwner()))
                .collect(Collectors.toList());
        int totalVisitors = sortedVisitors.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalVisitors / PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;
        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.translateColors(GUI_TITLE));
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Nombre total de visiteurs uniques: &b" + totalVisitors);
        infoLore.add("&7Nombre total de visites: &b" + stats.getVisits());
        infoLore.add("");
        infoLore.add("&7Page: &b" + (page + 1) + "&7/&b" + totalPages);
        ItemStack infoItem = guiManager.createGuiItem(Material.BOOK, "&e📊 &bStatistiques des visiteurs", infoLore);
        inventory.setItem(4, infoItem);
        List<String> legendLore = new ArrayList<>();
        legendLore.add("&a✓ &7= Accès autorisé");
        legendLore.add("&c✖ &7= Accès refusé");
        legendLore.add("&c⛔ &7= Banni définitivement");
        legendLore.add("&e⏱ &7= Banni temporairement");
        ItemStack legendItem = guiManager.createGuiItem(Material.KNOWLEDGE_BOOK, "&b❓ &bLégende", legendLore);
        inventory.setItem(6, legendItem);
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalVisitors);
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Integer> entry = sortedVisitors.get(i);
            UUID visitorId = entry.getKey();
            int visits = entry.getValue();
            int slot = 9 + (i - startIndex);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(visitorId);
            String name = offlinePlayer.getName();
            if (name == null) name = visitorId.toString().substring(0, 8);
            String nameColor = "&b";
            String statusSymbol = "&a✓ ";
            if (mineAccess.isPermanentlyBanned(visitorId)) {
                nameColor = "&4";
                statusSymbol = "&c⛔ ";
            } else if (mineAccess.isTemporarilyBanned(visitorId)) {
                nameColor = "&c";
                statusSymbol = "&e⏱ ";
            } else if (mineAccess.isDenied(visitorId)) {
                nameColor = "&8";
                statusSymbol = "&c✖ ";
            }
            ItemStack visitorItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) visitorItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.translateColors(statusSymbol + nameColor + name));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.translateColors("&7Visites: &b" + visits));
                lore.add("");
                if (mineAccess.isPermanentlyBanned(visitorId)) {
                    lore.add(ColorUtil.translateColors("&c⛔ &4Banni définitivement"));
                } else if (mineAccess.isTemporarilyBanned(visitorId)) {
                    long banExpiration = mineAccess.getBanExpiration(visitorId);
                    long remainingSeconds = (banExpiration - System.currentTimeMillis()) / 1000;
                    String formattedTime = formatTimeRemaining(remainingSeconds);
                    lore.add(ColorUtil.translateColors("&e⏱ &cBanni temporairement"));
                    lore.add(ColorUtil.translateColors("&7Temps restant: &e" + formattedTime));
                } else if (mineAccess.isDenied(visitorId)) {
                    lore.add(ColorUtil.translateColors("&c✖ &8Accès refusé"));
                } else {
                    lore.add(ColorUtil.translateColors("&a✓ &2Accès autorisé"));
                }
                lore.add("");
                lore.add(ColorUtil.translateColors("&eCliquez pour gérer ce visiteur"));
                meta.setLore(lore);
                meta.setOwningPlayer(offlinePlayer);
                visitorItem.setItemMeta(meta);
            }
            inventory.setItem(slot, visitorItem);
        }
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
        MineAccess mineAccess = ownerMine.getMineAccess();
        boolean isBanned = mineAccess.isBanned(targetId);
        boolean isPermanentlyBanned = mineAccess.isPermanentlyBanned(targetId);
        boolean isTemporarilyBanned = mineAccess.isTemporarilyBanned(targetId);
        boolean isDenied = mineAccess.isDenied(targetId);
        Inventory inventory = Bukkit.createInventory(null, 36, ColorUtil.translateColors("&8■ &bActions pour " + targetName + " &8■"));
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerInfo.getItemMeta();
        if (playerMeta != null) {
            playerMeta.setDisplayName(ColorUtil.translateColors("&b" + targetName));
            List<String> playerLore = new ArrayList<>();
            String statusText = "&a✓ &2Accès autorisé";
            if (isPermanentlyBanned) {
                statusText = "&c⛔ &4Banni définitivement";
            } else if (isTemporarilyBanned) {
                long banExpiration = mineAccess.getBanExpiration(targetId);
                long remainingSeconds = (banExpiration - System.currentTimeMillis()) / 1000;
                String formattedTime = formatTimeRemaining(remainingSeconds);
                statusText = "&e⏱ &cBanni temporairement &7(" + formattedTime + ")";
            } else if (isDenied) {
                statusText = "&c✖ &8Accès refusé";
            }
            playerLore.add(ColorUtil.translateColors("&7Statut: " + statusText));
            playerLore.add("");
            playerLore.add(ColorUtil.translateColors("&7Choisissez une action ci-dessous"));
            playerMeta.setLore(playerLore);
            playerMeta.setOwningPlayer(targetPlayer);
            playerInfo.setItemMeta(playerMeta);
        }
        inventory.setItem(4, playerInfo);
        inventory.setItem(10, guiManager.createGuiItem(Material.IRON_DOOR, "&e⚡ &cExpulser", Arrays.asList("&7Expulse le joueur de votre mine", "&7s'il est actuellement présent")));
        inventory.setItem(12, guiManager.createGuiItem(isPermanentlyBanned ? Material.GRAY_DYE : Material.RED_DYE, isPermanentlyBanned ? "&8⛔ &7Déjà banni" : "&c⛔ &cBannir définitivement", Arrays.asList(isPermanentlyBanned ? "&7Le joueur est déjà banni définitivement" : "&7Bannit définitivement ce joueur de votre mine")));
        inventory.setItem(14, guiManager.createGuiItem(isBanned ? Material.GRAY_DYE : Material.ORANGE_DYE, isBanned ? "&8⏱ &7Déjà banni" : "&e⏱ &eBannir pour 1 heure", Arrays.asList(isBanned ? "&7Le joueur est déjà banni" : "&7Bannit ce joueur pour 1 heure")));
        inventory.setItem(16, guiManager.createGuiItem(isBanned ? Material.GRAY_DYE : Material.YELLOW_DYE, isBanned ? "&8⏳ &7Déjà banni" : "&e⏳ &eBannir pour 24 heures", Arrays.asList(isBanned ? "&7Le joueur est déjà banni" : "&7Bannit ce joueur pour 24 heures")));
        inventory.setItem(20, guiManager.createGuiItem(!isBanned ? Material.GRAY_DYE : Material.LIME_DYE, !isBanned ? "&8✓ &7Pas banni" : "&a✓ &aDébannir", Arrays.asList(!isBanned ? "&7Ce joueur n'est pas banni" : "&7Retire le bannissement de ce joueur")));
        inventory.setItem(22, guiManager.createGuiItem(isDenied ? Material.GRAY_CONCRETE : Material.RED_CONCRETE, isDenied ? "&8☒ &7Déjà refusé" : "&c☒ &cRefuser l'accès", Arrays.asList(isDenied ? "&7Ce joueur a déjà un accès refusé" : "&7Refuse l'accès à ce joueur")));
        inventory.setItem(24, guiManager.createGuiItem(!isDenied ? Material.GRAY_CONCRETE : Material.LIME_CONCRETE, !isDenied ? "&8☑ &7Déjà autorisé" : "&a☑ &aAutoriser l'accès", Arrays.asList(!isDenied ? "&7Ce joueur a déjà accès" : "&7Autorise l'accès à ce joueur")));
        inventory.setItem(28, guiManager.createGuiItem(Material.EMERALD, "&aAjouter comme contributeur", Arrays.asList("&7Ce joueur pourra gérer la mine comme un co-propriétaire.", "", "&eCliquez pour ajouter comme contributeur")));
        inventory.setItem(31, guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour", Arrays.asList("&7Retourner à la liste des visiteurs")));
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, ACTION_TYPE + ":" + targetId.toString());
    }

    private static String formatTimeRemaining(long seconds) {
        if (seconds <= 0) return "0s";
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("j ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0) result.append(seconds).append("s");
        return result.toString().trim();
    }
} 