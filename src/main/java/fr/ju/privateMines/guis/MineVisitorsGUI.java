package fr.ju.privateMines.guis;
import java.util.ArrayList;
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
            if (name == null) name = "Joueur inconnu";
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
            ItemStack previousItem = guiManager.createGuiItem(Material.ARROW, "&e◀ &bPage précédente", 
                    "&7Aller à la page " + page);
            inventory.setItem(45, previousItem);
        }
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour", 
                "&7Retourner au menu principal");
        inventory.setItem(49, backItem);
        if (page < totalPages - 1) {
            ItemStack nextItem = guiManager.createGuiItem(Material.ARROW, "&e▶ &bPage suivante", 
                    "&7Aller à la page " + (page + 2));
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
        if (targetName == null) targetName = "Joueur inconnu";
        Mine targetMine = plugin.getMineManager().getMine(targetPlayer.getUniqueId()).orElse(null);
        if (targetMine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de la mine du joueur."));
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 36, 
                ColorUtil.translateColors("&8■ &bActions pour " + targetName + " &8■"));
        MineAccess mineAccess = targetMine.getMineAccess();
        boolean isBanned = mineAccess.isBanned(targetId);
        boolean isPermanentlyBanned = mineAccess.isPermanentlyBanned(targetId);
        boolean isTemporarilyBanned = mineAccess.isTemporarilyBanned(targetId);
        boolean isDenied = mineAccess.isDenied(targetId);
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
        List<String> kickLore = new ArrayList<>();
        kickLore.add("&7Expulse le joueur de votre mine");
        kickLore.add("&7s'il est actuellement présent");
        ItemStack kickItem = guiManager.createGuiItem(Material.IRON_DOOR, "&e⚡ &cExpulser", kickLore);
        inventory.setItem(10, kickItem);
        List<String> permBanLore = new ArrayList<>();
        if (isPermanentlyBanned) {
            permBanLore.add("&7Le joueur est déjà banni");
            permBanLore.add("&7définitivement de votre mine");
        } else {
            permBanLore.add("&7Bannit définitivement ce joueur");
            permBanLore.add("&7de votre mine");
            permBanLore.add("");
            permBanLore.add("&7Le joueur ne pourra plus accéder");
            permBanLore.add("&7à votre mine tant que vous ne");
            permBanLore.add("&7l'aurez pas débanni");
        }
        Material permBanMat = isPermanentlyBanned ? Material.GRAY_DYE : Material.RED_DYE;
        ItemStack permBanItem = guiManager.createGuiItem(permBanMat, 
                isPermanentlyBanned ? "&8⛔ &7Déjà banni" : "&c⛔ &cBannir définitivement", 
                permBanLore);
        inventory.setItem(12, permBanItem);
        List<String> hourBanLore = new ArrayList<>();
        if (isBanned) {
            hourBanLore.add("&7Le joueur est déjà banni");
            hourBanLore.add("&7de votre mine");
        } else {
            hourBanLore.add("&7Bannit ce joueur de votre mine");
            hourBanLore.add("&7pour une durée d'une heure");
        }
        Material hourBanMat = isBanned ? Material.GRAY_DYE : Material.ORANGE_DYE;
        ItemStack hourBanItem = guiManager.createGuiItem(hourBanMat, 
                isBanned ? "&8⏱ &7Déjà banni" : "&e⏱ &eBannir pour 1 heure", 
                hourBanLore);
        inventory.setItem(14, hourBanItem);
        List<String> dayBanLore = new ArrayList<>();
        if (isBanned) {
            dayBanLore.add("&7Le joueur est déjà banni");
            dayBanLore.add("&7de votre mine");
        } else {
            dayBanLore.add("&7Bannit ce joueur de votre mine");
            dayBanLore.add("&7pour une durée de 24 heures");
        }
        Material dayBanMat = isBanned ? Material.GRAY_DYE : Material.YELLOW_DYE;
        ItemStack dayBanItem = guiManager.createGuiItem(dayBanMat, 
                isBanned ? "&8⏳ &7Déjà banni" : "&e⏳ &eBannir pour 24 heures", 
                dayBanLore);
        inventory.setItem(16, dayBanItem);
        List<String> unbanLore = new ArrayList<>();
        if (!isBanned) {
            unbanLore.add("&7Ce joueur n'est pas actuellement");
            unbanLore.add("&7banni de votre mine");
        } else {
            unbanLore.add("&7Retire le bannissement");
            unbanLore.add("&7de ce joueur");
            unbanLore.add("");
            unbanLore.add("&7Le joueur pourra à nouveau");
            unbanLore.add("&7visiter votre mine");
        }
        Material unbanMat = !isBanned ? Material.GRAY_DYE : Material.LIME_DYE;
        ItemStack unbanItem = guiManager.createGuiItem(unbanMat, 
                !isBanned ? "&8✓ &7Pas banni" : "&a✓ &aDébannir", 
                unbanLore);
        inventory.setItem(20, unbanItem);
        List<String> denyLore = new ArrayList<>();
        if (isDenied) {
            denyLore.add("&7Ce joueur a déjà un accès");
            denyLore.add("&7refusé à votre mine");
        } else {
            denyLore.add("&7Refuse l'accès à ce joueur");
            denyLore.add("&7sans le bannir");
            denyLore.add("");
            denyLore.add("&7Le joueur ne pourra pas visiter");
            denyLore.add("&7votre mine mais ne sera pas banni");
        }
        Material denyMat = isDenied ? Material.GRAY_CONCRETE : Material.RED_CONCRETE;
        ItemStack denyItem = guiManager.createGuiItem(denyMat, 
                isDenied ? "&8☒ &7Déjà refusé" : "&c☒ &cRefuser l'accès", 
                denyLore);
        inventory.setItem(22, denyItem);
        List<String> allowLore = new ArrayList<>();
        if (!isDenied) {
            allowLore.add("&7Ce joueur a déjà accès");
            allowLore.add("&7à votre mine");
        } else {
            allowLore.add("&7Autorise l'accès à ce joueur");
            allowLore.add("&7qui était précédemment refusé");
        }
        Material allowMat = !isDenied ? Material.GRAY_CONCRETE : Material.LIME_CONCRETE;
        ItemStack allowItem = guiManager.createGuiItem(allowMat, 
                !isDenied ? "&8☑ &7Déjà autorisé" : "&a☑ &aAutoriser l'accès", 
                allowLore);
        inventory.setItem(24, allowItem);
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour", 
                "&7Retourner à la liste des visiteurs");
        inventory.setItem(31, backItem);
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