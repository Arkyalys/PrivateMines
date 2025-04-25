package fr.ju.privateMines.placeholders;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
public class PrivateMinesPlaceholders extends PlaceholderExpansion {
    private final PrivateMines plugin;
    public PrivateMinesPlaceholders(PrivateMines plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Placeholders PrivateMines enregistrés avec l'identifiant 'privatemine'");
    }
    @Override
    public String getIdentifier() {
        return "privatemine";
    }
    @Override
    public String getAuthor() {
        return "JuMine";
    }
    @Override
    public String getVersion() {
        return "1.0";
    }
    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (identifier.equals("has_mine")) {
            return plugin.getMineManager().hasMine(player.getUniqueId()) ? "true" : "false";
        }
        Mine mine = plugin.getMineManager().getMine(player.getUniqueId()).orElse(null);
        if (mine == null) {
            return handleNullMine(identifier);
        } else {
            String result = handleMinePlaceholders(mine, identifier);
            if (result != null) return result;
        }
        if (identifier.startsWith("top_")) {
            return handleTopPlaceholders(identifier);
        }
        if (identifier.equals("count")) {
            return String.valueOf(plugin.getMineManager().mineMemoryService.getPlayerMines().size());
        }
        return null;
    }
    private String handleNullMine(String identifier) {
        switch (identifier) {
            case "type":
            case "tier":
            case "size":
            case "tax":
            case "is_open":
            case "blocks_mined":
            case "total_blocks":
            case "percentage_mined":
            case "visits":
            case "last_reset":
            case "owner":
            case "location":
            case "teleport_x":
            case "teleport_y":
            case "teleport_z":
            case "teleport_world":
            case "status_color":
            case "progress_bar":
            case "next_reset":
            case "time_since_last_reset":
            case "block_ratio":
            case "is_full":
            case "visitor_last":
            case "visitor_count_unique":
            case "reset_count":
                return "N/A";
            default:
                return null;
        }
    }
    private String handleMinePlaceholders(Mine mine, String identifier) {
        switch (identifier) {
            case "type": return mine.getType();
            case "tier": return String.valueOf(mine.getTier());
            case "size": return String.valueOf(mine.getSize());
            case "tax": return String.valueOf(mine.getTax());
            case "is_open": return mine.isOpen() ? "Ouverte" : "Fermée";
            case "blocks_mined": return String.valueOf(mine.getStats().getBlocksMined());
            case "total_blocks": return String.valueOf(mine.getStats().getTotalBlocks());
            case "percentage_mined": return String.valueOf(mine.getStats().getPercentageMined());
            case "visits": return String.valueOf(mine.getStats().getVisits());
            case "last_reset": return formatDate(mine.getStats().getLastReset());
            case "owner": return Bukkit.getOfflinePlayer(mine.getOwner()).getName() != null ? Bukkit.getOfflinePlayer(mine.getOwner()).getName() : mine.getOwner().toString();
            case "location": return mine.getLocation().getWorld().getName() + ", " + mine.getLocation().getBlockX() + ", " + mine.getLocation().getBlockY() + ", " + mine.getLocation().getBlockZ();
            case "teleport_x": return mine.getTeleportLocation() != null ? String.valueOf(mine.getTeleportLocation().getBlockX()) : "N/A";
            case "teleport_y": return mine.getTeleportLocation() != null ? String.valueOf(mine.getTeleportLocation().getBlockY()) : "N/A";
            case "teleport_z": return mine.getTeleportLocation() != null ? String.valueOf(mine.getTeleportLocation().getBlockZ()) : "N/A";
            case "teleport_world": return mine.getTeleportLocation() != null && mine.getTeleportLocation().getWorld() != null ? mine.getTeleportLocation().getWorld().getName() : "N/A";
            case "status_color": return mine.isOpen() ? "§aOuverte" : "§cFermée";
            case "progress_bar": return buildProgressBar(mine);
            case "next_reset": return getNextReset(mine);
            case "time_since_last_reset": return getTimeSinceLastReset(mine);
            case "block_ratio": return mine.getStats().getBlocksMined() + "/" + mine.getStats().getTotalBlocks();
            case "is_full": return isMineFull(mine);
            case "visitor_last": return getLastVisitor(mine);
            case "visitor_count_unique": return String.valueOf(mine.getStats().getVisitorStats().size());
            case "reset_count": return getResetCount(mine);
            default: return null;
        }
    }
    private String buildProgressBar(Mine mine) {
        int percent = mine.getStats().getPercentageMined();
        int barLength = 20;
        int filled = (int) (barLength * (percent / 100.0));
        StringBuilder bar = new StringBuilder("§8[");
        for (int j = 0; j < barLength; j++) {
            bar.append(j < filled ? "§a■" : "§7■");
        }
        bar.append("§8]");
        return bar.toString();
    }
    private String getNextReset(Mine mine) {
        int threshold = plugin.getConfigManager().getConfig().getInt("Gameplay.auto-reset.threshold", 65);
        int percent = mine.getStats().getPercentageMined();
        return String.valueOf(Math.max(0, threshold - percent));
    }
    private String getTimeSinceLastReset(Mine mine) {
        long last = mine.getStats().getLastReset();
        long now = System.currentTimeMillis();
        long diff = now - last;
        long sec = diff / 1000;
        long min = sec / 60;
        long h = min / 60;
        min = min % 60;
        sec = sec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (min > 0) sb.append(min).append("m ");
        sb.append(sec).append("s");
        return sb.toString().trim();
    }
    private String isMineFull(Mine mine) {
        int threshold = plugin.getConfigManager().getConfig().getInt("Gameplay.auto-reset.threshold", 65);
        return mine.getStats().getPercentageMined() >= threshold ? "true" : "false";
    }
    private String getLastVisitor(Mine mine) {
        if (mine.getStats().getVisitorStats().isEmpty()) return "N/A";
        java.util.UUID last = null;
        long lastTime = 0;
        for (java.util.Map.Entry<java.util.UUID, Integer> entry : mine.getStats().getVisitorStats().entrySet()) {
            if (entry.getValue() > lastTime) {
                last = entry.getKey();
                lastTime = entry.getValue();
            }
        }
        return last != null ? Bukkit.getOfflinePlayer(last).getName() : "N/A";
    }
    private String getResetCount(Mine mine) {
        return String.valueOf((int) (System.currentTimeMillis() - mine.getStats().getLastReset()) / 1000);
    }
    private String handleTopPlaceholders(String identifier) {
        try {
            String[] parts = identifier.split("_");
            if (parts.length == 3) {
                int position = Integer.parseInt(parts[1]) - 1;
                String value = parts[2];
                java.util.List<Mine> topMines = plugin.getStatsManager().getTopMines();
                if (position >= 0 && position < topMines.size()) {
                    Mine topMine = topMines.get(position);
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(topMine.getOwner());
                    MineStats stats = topMine.getStats();
                    switch (value) {
                        case "name": return owner.getName() != null ? owner.getName() : "Inconnu";
                        case "blocks": return String.valueOf(stats.getBlocksMined());
                        case "visits": return String.valueOf(stats.getVisits());
                        case "tier": return String.valueOf(topMine.getTier());
                    }
                }
            }
        } catch (NumberFormatException e) {
            return "N/A";
        }
        return null;
    }
    private String formatDate(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(new Date(timestamp));
    }
} 