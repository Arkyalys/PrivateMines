package fr.ju.privateMines.commands.utils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
public class MineCommandUtils {
    public static void showPlayerStats(PrivateMines plugin, MineManager mineManager, ConfigManager configManager, Player viewer, UUID ownerUUID) {
        Mine mine = mineManager.getMine(ownerUUID).orElse(null);
        if (mine == null) {
            viewer.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        MineStats stats = mine.getStats();
        if (stats == null) {
            viewer.sendMessage(configManager.getMessageOrDefault("stats-none", "&cAucune statistique disponible pour cette mine."));
            return;
        }
        String ownerName = plugin.getServer().getOfflinePlayer(ownerUUID).getName();
        Map<String, String> rep = Map.of("%player%", ownerName != null ? ownerName : ownerUUID.toString(), "%mined%", String.valueOf(stats.getBlocksMined()), "%total%", String.valueOf(stats.getTotalBlocks()), "%percentage%", String.valueOf(stats.getPercentageMined()), "%visits%", String.valueOf(stats.getVisits()), "%time%", formatTimestamp(stats.getLastReset()));
        viewer.sendMessage(configManager.getMessage("stats-header", Map.of("%player%", ownerName != null ? ownerName : ownerUUID.toString())));
        viewer.sendMessage(configManager.getMessage("stats-blocks", rep));
        viewer.sendMessage(configManager.getMessage("stats-visits", rep));
        viewer.sendMessage(configManager.getMessage("stats-last-reset", rep));
    }
    public static void showTopStats(PrivateMines plugin, MineManager mineManager, Player viewer) {
        Map<UUID, Mine> mines = mineManager.mineMemoryService.getPlayerMines();
        if (mines.isEmpty()) {
            viewer.sendMessage(configManager.getMessageOrDefault("stats-no-mines", "&cAucune mine n'a été créée."));
            return;
        }
        List<Mine> sortedMines = new ArrayList<>(mines.values());
        sortedMines.sort((m1, m2) -> Integer.compare(m2.getStats().getBlocksMined(), m1.getStats().getBlocksMined()));
        viewer.sendMessage(configManager.getMessage("stats-top-header"));
        int count = 0;
        for (Mine mine : sortedMines) {
            if (count >= 5) break;
            String ownerName = plugin.getServer().getOfflinePlayer(mine.getOwner()).getName();
            Map<String, String> rep = Map.of("%position%", String.valueOf(count + 1), "%player%", ownerName != null ? ownerName : mine.getOwner().toString(), "%blocks%", String.valueOf(mine.getStats().getBlocksMined()));
            viewer.sendMessage(configManager.getMessage("stats-top-entry", rep));
            count++;
        }
    }
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(date);
    }
    public static void sendHelp(PrivateMines plugin, MineManager mineManager, ConfigManager configManager, Player player) {
        player.sendMessage(configManager.getMessage("help-title"));
        boolean isAdmin = player.hasPermission("mine.admin");
        // Commandes de base pour tous
        player.sendMessage(configManager.getMessage("help-create"));
        player.sendMessage(configManager.getMessage("help-delete"));
        player.sendMessage(configManager.getMessage("help-reset"));
        player.sendMessage(configManager.getMessage("help-teleport"));
        player.sendMessage(configManager.getMessage("help-visit"));
        player.sendMessage(configManager.getMessage("help-gui"));
        // Commandes admin si admin
        if (isAdmin) {
            player.sendMessage(configManager.getMessage("help-expand"));
            player.sendMessage(configManager.getMessage("help-upgrade"));
            player.sendMessage(configManager.getMessage("help-settype"));
            player.sendMessage(configManager.getMessage("help-settier"));
            player.sendMessage(configManager.getMessage("help-tax"));
            player.sendMessage(configManager.getMessage("help-ban"));
            player.sendMessage(configManager.getMessage("help-unban"));
            player.sendMessage(configManager.getMessage("help-kick"));
            player.sendMessage(configManager.getMessage("help-menu"));
            player.sendMessage(configManager.getMessage("help-pregen"));
            player.sendMessage(configManager.getMessage("help-reload"));
            player.sendMessage(configManager.getMessage("help-savestats"));
            player.sendMessage(configManager.getMessage("help-admin-reset"));
        }
    }
    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return formatTimeUnit(seconds, "seconde");
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return formatTimeUnit(minutes, "minute");
        }
        
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours < 24) {
            return formatCompoundDuration(hours, "heure", minutes, "minute");
        }
        
        long days = hours / 24;
        hours = hours % 24;
        
        return formatCompoundDuration(days, "jour", hours, "heure");
    }
    
    /**
     * Formate une unité de temps avec le mot approprié (singulier ou pluriel)
     */
    private static String formatTimeUnit(long value, String unit) {
        return value + " " + pluralize(value, unit);
    }
    
    /**
     * Retourne la forme singulier ou pluriel d'une unité de temps
     */
    private static String pluralize(long value, String unit) {
        return value == 1 ? unit : unit + "s";
    }
    
    /**
     * Formate une durée composée de deux unités de temps
     */
    private static String formatCompoundDuration(long primaryValue, String primaryUnit,
                                                long secondaryValue, String secondaryUnit) {
        String result = formatTimeUnit(primaryValue, primaryUnit);
        
        if (secondaryValue > 0) {
            result += " " + formatTimeUnit(secondaryValue, secondaryUnit);
        }
        
        return result;
    }
    public static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }
        
        // Si la durée est juste un nombre entier, retournez-le tel quel
        if (durationStr.matches("\\d+")) {
            return Long.parseLong(durationStr);
        }
        
        return parseDurationWithUnits(durationStr);
    }

    private static long parseDurationWithUnits(String durationStr) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([dhms])");
        java.util.regex.Matcher matcher = pattern.matcher(durationStr.toLowerCase());
        
        long totalSeconds = 0;
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            int amount = Integer.parseInt(matcher.group(1));
            totalSeconds += convertToSeconds(amount, matcher.group(2));
        }
        
        if (!found) {
            throw new IllegalArgumentException("Invalid duration format");
        }
        
        return totalSeconds;
    }

    private static long convertToSeconds(int amount, String unit) {
        switch (unit) {
            case "d": return amount * 86400;
            case "h": return amount * 3600;
            case "m": return amount * 60;
            case "s": return amount;
            default: return 0; // Ne devrait jamais arriver car le pattern est limité à d, h, m, s
        }
    }
} 