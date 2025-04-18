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
            viewer.sendMessage(ColorUtil.translateColors("&cAucune statistique disponible pour cette mine."));
            return;
        }
        String ownerName = plugin.getServer().getOfflinePlayer(ownerUUID).getName();
        viewer.sendMessage(ColorUtil.translateColors("&6&lStatistiques de la mine de " + (ownerName != null ? ownerName : ownerUUID.toString())));
        viewer.sendMessage(ColorUtil.translateColors("&7Blocs minés: &f" + stats.getBlocksMined() + " &7/ &f" + stats.getTotalBlocks() + " &7(&f" + stats.getPercentageMined() + "%&7)"));
        viewer.sendMessage(ColorUtil.translateColors("&7Visites: &f" + stats.getVisits()));
        viewer.sendMessage(ColorUtil.translateColors("&7Dernier reset: &f" + formatTimestamp(stats.getLastReset())));
    }
    public static void showTopStats(PrivateMines plugin, MineManager mineManager, Player viewer) {
        Map<UUID, Mine> mines = mineManager.mineMemoryService.getPlayerMines();
        if (mines.isEmpty()) {
            viewer.sendMessage(ColorUtil.translateColors("&cAucune mine n'a été créée."));
            return;
        }
        List<Mine> sortedMines = new ArrayList<>(mines.values());
        sortedMines.sort((m1, m2) -> Integer.compare(m2.getStats().getBlocksMined(), m1.getStats().getBlocksMined()));
        viewer.sendMessage(ColorUtil.translateColors("&6&lTop 5 des mines les plus actives"));
        int count = 0;
        for (Mine mine : sortedMines) {
            if (count >= 5) break;
            String ownerName = plugin.getServer().getOfflinePlayer(mine.getOwner()).getName();
            viewer.sendMessage(ColorUtil.translateColors("&7#" + (count + 1) + " &f" + (ownerName != null ? ownerName : mine.getOwner().toString()) + " &7- &f" + mine.getStats().getBlocksMined() + " &7blocs minés"));
            count++;
        }
    }
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(date);
    }
    public static void sendHelp(PrivateMines plugin, MineManager mineManager, ConfigManager configManager, Player player) {
        player.sendMessage(ColorUtil.translateColors("&6&lPrivateMines &7- &fAvailable commands:"));
        player.sendMessage(ColorUtil.translateColors("&7/mine create &f- Create your private mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine delete &f- Delete your private mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine reset &f- Reset your mine blocks"));
        player.sendMessage(ColorUtil.translateColors("&7/mine expand &f- Expand the size of your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine upgrade &f- Upgrade your mine to the next tier"));
        player.sendMessage(ColorUtil.translateColors("&7/mine settype <type> &f- Change your mine type"));
        player.sendMessage(ColorUtil.translateColors("&7/mine settier <tier> &f- Change your mine tier (Admin)"));
        player.sendMessage(ColorUtil.translateColors("&7/mine tax <percent> &f- Set the tax percentage of your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine teleport &f- Teleport to your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine tp &f- Shortcut for /mine teleport"));
        player.sendMessage(ColorUtil.translateColors("&7/mine visit <player> &f- Visit another player's mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine kick <player> &f- Kick a player from your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine ban <player> [duration] &f- Ban a player from your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine unban <player> &f- Unban a player from your mine"));
        player.sendMessage(ColorUtil.translateColors("&7/mine gui &f- Open the mine management menu"));
        player.sendMessage(ColorUtil.translateColors("&7/mine menu &f- Shortcut for /mine gui"));
        player.sendMessage(ColorUtil.translateColors("&7/mine pregen <number> [type] &f- Pre-generate mines (Admin)"));
    }
    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " " + (seconds == 1 ? "seconde" : "secondes");
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " " + (minutes == 1 ? "minute" : "minutes");
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            String result = hours + " " + (hours == 1 ? "heure" : "heures");
            if (minutes > 0) {
                result += " " + minutes + " " + (minutes == 1 ? "minute" : "minutes");
            }
            return result;
        }
        long days = hours / 24;
        hours = hours % 24;
        String result = days + " " + (days == 1 ? "jour" : "jours");
        if (hours > 0) {
            result += " " + hours + " " + (hours == 1 ? "heure" : "heures");
        }
        return result;
    }
    public static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }
        if (durationStr.matches("\\d+")) {
            return Long.parseLong(durationStr);
        }
        long totalSeconds = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([dhms])");
        java.util.regex.Matcher matcher = pattern.matcher(durationStr.toLowerCase());
        boolean found = false;
        while (matcher.find()) {
            found = true;
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d":
                    totalSeconds += amount * 86400;
                    break;
                case "h":
                    totalSeconds += amount * 3600;
                    break;
                case "m":
                    totalSeconds += amount * 60;
                    break;
                case "s":
                    totalSeconds += amount;
                    break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Invalid duration format");
        }
        return totalSeconds;
    }
} 