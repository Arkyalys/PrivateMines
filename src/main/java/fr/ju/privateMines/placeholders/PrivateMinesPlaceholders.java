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
    private static final String[] NULL_MINE_PLACEHOLDERS = {
        "tier", "size", "tax", "is_open", "blocks_mined", "total_blocks",
        "percentage_mined", "visits", "last_reset", "owner", "location",
        "teleport_x", "teleport_y", "teleport_z", "teleport_world",
        "status_color", "progress_bar", "next_reset", "time_since_last_reset",
        "block_ratio", "is_full", "visitor_last", "visitor_count_unique",
        "reset_count"
    };
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
        for (String placeholder : NULL_MINE_PLACEHOLDERS) {
            if (identifier.equals(placeholder)) {
                return "N/A";
            }
        }
        return null;
    }
    private String handleMinePlaceholders(Mine mine, String identifier) {
        // Gestion des informations basiques de la mine
        String basicInfo = handleBasicMineInfo(mine, identifier);
        if (basicInfo != null) return basicInfo;
        
        // Gestion des informations de statistiques
        String statsInfo = handleMineStatsInfo(mine, identifier);
        if (statsInfo != null) return statsInfo;
        
        // Gestion des informations de localisation
        String locationInfo = handleMineLocationInfo(mine, identifier);
        if (locationInfo != null) return locationInfo;
        
        // Gestion des informations de statut et d'affichage
        String displayInfo = handleMineDisplayInfo(mine, identifier);
        if (displayInfo != null) return displayInfo;
        
        return null;
    }
    private String handleBasicMineInfo(Mine mine, String identifier) {
        switch (identifier) {
            case "tier": return String.valueOf(mine.getTier());
            case "size": return String.valueOf(mine.getSize());
            case "tax": return String.valueOf(mine.getTax());
            case "is_open": return mine.isOpen() ? "Ouverte" : "Fermée";
            case "owner": return Bukkit.getOfflinePlayer(mine.getOwner()).getName() != null ? 
                          Bukkit.getOfflinePlayer(mine.getOwner()).getName() : mine.getOwner().toString();
            default: return null;
        }
    }
    private String handleMineStatsInfo(Mine mine, String identifier) {
        MineStats stats = mine.getStats();
        switch (identifier) {
            case "blocks_mined": return String.valueOf(stats.getBlocksMined());
            case "total_blocks": return String.valueOf(stats.getTotalBlocks());
            case "percentage_mined": return String.valueOf(stats.getPercentageMined());
            case "visits": return String.valueOf(stats.getVisits());
            case "last_reset": return formatDate(stats.getLastReset());
            case "block_ratio": return stats.getBlocksMined() + "/" + stats.getTotalBlocks();
            case "reset_count": return getResetCount(mine);
            case "visitor_last": return getLastVisitor(mine);
            case "visitor_count_unique": return String.valueOf(stats.getVisitorStats().size());
            default: return null;
        }
    }
    private String handleMineLocationInfo(Mine mine, String identifier) {
        switch (identifier) {
            case "location": 
                return mine.getLocation().getWorld().getName() + ", " + 
                       mine.getLocation().getBlockX() + ", " + 
                       mine.getLocation().getBlockY() + ", " + 
                       mine.getLocation().getBlockZ();
            case "teleport_x": 
                return mine.getTeleportLocation() != null ? 
                       String.valueOf(mine.getTeleportLocation().getBlockX()) : "N/A";
            case "teleport_y": 
                return mine.getTeleportLocation() != null ? 
                       String.valueOf(mine.getTeleportLocation().getBlockY()) : "N/A";
            case "teleport_z": 
                return mine.getTeleportLocation() != null ? 
                       String.valueOf(mine.getTeleportLocation().getBlockZ()) : "N/A";
            case "teleport_world": 
                return mine.getTeleportLocation() != null && mine.getTeleportLocation().getWorld() != null ? 
                       mine.getTeleportLocation().getWorld().getName() : "N/A";
            default: return null;
        }
    }
    private String handleMineDisplayInfo(Mine mine, String identifier) {
        switch (identifier) {
            case "status_color": return mine.isOpen() ? "§aOuverte" : "§cFermée";
            case "progress_bar": return buildProgressBar(mine);
            case "next_reset": return getNextReset(mine);
            case "time_since_last_reset": return getTimeSinceLastReset(mine);
            case "is_full": return isMineFull(mine);
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