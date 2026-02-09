package fr.ju.privateMines.placeholders;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ProgressBarUtil;
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
        
        // Grouper par catégories pour réduire la complexité
        if (isBasicStat(identifier)) {
            return getStatValueAsString(stats, getBasicStatType(identifier));
        }
        
        if (isVisitorStat(identifier)) {
            return getVisitorStatValue(mine, identifier);
        }
        
        if (isTimeStat(identifier)) {
            return getTimeStatValue(mine, stats, identifier);
        }
        
        // Gérer le cas spécifique du ratio de blocs
        if ("block_ratio".equals(identifier)) {
            return getBlockRatio(stats);
        }
        
        return null;
    }
    
    /**
     * Vérifie si l'identifiant correspond à une statistique de base
     */
    private boolean isBasicStat(String identifier) {
        return "blocks_mined".equals(identifier) || 
               "total_blocks".equals(identifier) || 
               "percentage_mined".equals(identifier) || 
               "visits".equals(identifier);
    }
    
    /**
     * Récupère le type de statistique de base correspondant à l'identifiant
     */
    private StatsValueType getBasicStatType(String identifier) {
        switch (identifier) {
            case "blocks_mined": return StatsValueType.BLOCKS_MINED;
            case "total_blocks": return StatsValueType.TOTAL_BLOCKS;
            case "percentage_mined": return StatsValueType.PERCENTAGE_MINED;
            case "visits": return StatsValueType.VISITS;
            default: throw new IllegalArgumentException("Identifiant de statistique non supporté: " + identifier);
        }
    }
    
    /**
     * Vérifie si l'identifiant correspond à une statistique liée aux visiteurs
     */
    private boolean isVisitorStat(String identifier) {
        return "visitor_last".equals(identifier) || 
               "visitor_count_unique".equals(identifier);
    }
    
    /**
     * Récupère la valeur d'une statistique liée aux visiteurs
     */
    private String getVisitorStatValue(Mine mine, String identifier) {
        switch (identifier) {
            case "visitor_last": return getLastVisitor(mine);
            case "visitor_count_unique": return String.valueOf(mine.getStats().getVisitorStats().size());
            default: return "N/A";
        }
    }
    
    /**
     * Vérifie si l'identifiant correspond à une statistique liée au temps
     */
    private boolean isTimeStat(String identifier) {
        return "last_reset".equals(identifier) || 
               "reset_count".equals(identifier);
    }
    
    /**
     * Récupère la valeur d'une statistique liée au temps
     */
    private String getTimeStatValue(Mine mine, MineStats stats, String identifier) {
        switch (identifier) {
            case "last_reset": return formatDate(stats.getLastReset());
            case "reset_count": return getResetCount(mine);
            default: return "N/A";
        }
    }
    private String handleMineLocationInfo(Mine mine, String identifier) {
        switch (identifier) {
            case "location": return getFormattedMineLocation(mine);
            case "teleport_x": return getTeleportCoordinate(mine, CoordinateType.X);
            case "teleport_y": return getTeleportCoordinate(mine, CoordinateType.Y);
            case "teleport_z": return getTeleportCoordinate(mine, CoordinateType.Z);
            case "teleport_world": return getTeleportWorld(mine);
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
        return ProgressBarUtil.createProgressBar(mine.getStats().getPercentageMined());
    }
    private String getNextReset(Mine mine) {
        int threshold = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.auto-reset.threshold", 65);
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
        int threshold = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.auto-reset.threshold", 65);
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
            PlaceholderComponents components = parseTopPlaceholderIdentifier(identifier);
            if (components == null) {
                return null;
            }
            
            int position = components.position;
            String valueType = components.valueType;
            
            Mine topMine = getTopMineAtPosition(position);
            if (topMine == null) {
                return "N/A";
            }
            
            return getTopMineValue(topMine, valueType);
        } catch (NumberFormatException e) {
            return "N/A";
        }
    }
    private String formatDate(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return format.format(new Date(timestamp));
    }
    /**
     * Type énuméré pour les coordonnées
     */
    private enum CoordinateType {
        X, Y, Z
    }
    
    /**
     * Retourne la position formatée de la mine
     */
    private String getFormattedMineLocation(Mine mine) {
        return mine.getLocation().getWorld().getName() + ", " + 
               mine.getLocation().getBlockX() + ", " + 
               mine.getLocation().getBlockY() + ", " + 
               mine.getLocation().getBlockZ();
    }
    
    /**
     * Récupère une coordonnée spécifique du point de téléportation
     */
    private String getTeleportCoordinate(Mine mine, CoordinateType type) {
        if (!hasTeleportLocation(mine)) {
            return "N/A";
        }
        
        switch (type) {
            case X: return String.valueOf(mine.getTeleportLocation().getBlockX());
            case Y: return String.valueOf(mine.getTeleportLocation().getBlockY());
            case Z: return String.valueOf(mine.getTeleportLocation().getBlockZ());
            default: return "N/A";
        }
    }
    
    /**
     * Récupère le monde du point de téléportation
     */
    private String getTeleportWorld(Mine mine) {
        if (!hasTeleportLocationWithWorld(mine)) {
            return "N/A";
        }
        return mine.getTeleportLocation().getWorld().getName();
    }
    
    /**
     * Vérifie si la mine a un point de téléportation valide
     */
    private boolean hasTeleportLocation(Mine mine) {
        return mine.getTeleportLocation() != null;
    }
    
    /**
     * Vérifie si la mine a un point de téléportation avec un monde valide
     */
    private boolean hasTeleportLocationWithWorld(Mine mine) {
        return hasTeleportLocation(mine) && mine.getTeleportLocation().getWorld() != null;
    }
    
    /**
     * Retourne le ratio de blocs minés sous forme de chaîne
     */
    private String getBlockRatio(MineStats stats) {
        return stats.getBlocksMined() + "/" + stats.getTotalBlocks();
    }
    
    /**
     * Structure pour stocker les composants d'un placeholder de type "top_X_value"
     */
    private static class PlaceholderComponents {
        final int position;
        final String valueType;
        
        PlaceholderComponents(int position, String valueType) {
            this.position = position;
            this.valueType = valueType;
        }
    }
    
    /**
     * Parse l'identifiant du placeholder pour extraire la position et le type de valeur
     * @return PlaceholderComponents ou null si le format est invalide
     */
    private PlaceholderComponents parseTopPlaceholderIdentifier(String identifier) {
        String[] parts = identifier.split("_");
        if (parts.length != 3) {
            return null;
        }
        
        try {
            int position = Integer.parseInt(parts[1]) - 1;
            String valueType = parts[2];
            return new PlaceholderComponents(position, valueType);
        } catch (NumberFormatException e) {
            throw e; // Rethrow pour être géré dans la méthode appelante
        }
    }
    
    /**
     * Récupère la mine à une position donnée dans le classement
     * @return La mine à la position spécifiée ou null si hors limites
     */
    private Mine getTopMineAtPosition(int position) {
        java.util.List<Mine> topMines = plugin.getStatsManager().getTopMines();
        if (position < 0 || position >= topMines.size()) {
            return null;
        }
        return topMines.get(position);
    }
    
    /**
     * Récupère la valeur spécifique demandée pour une mine du top
     */
    private String getTopMineValue(Mine topMine, String valueType) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(topMine.getOwner());
        MineStats stats = topMine.getStats();
        
        switch (valueType) {
            case "name": return owner.getName() != null ? owner.getName() : "Inconnu";
            case "blocks": return String.valueOf(stats.getBlocksMined());
            case "visits": return String.valueOf(stats.getVisits());
            case "tier": return String.valueOf(topMine.getTier());
            default: return "N/A";
        }
    }
    
    /**
     * Énumération pour les différents types de valeurs statistiques
     */
    private enum StatsValueType {
        BLOCKS_MINED, TOTAL_BLOCKS, PERCENTAGE_MINED, VISITS
    }
    
    /**
     * Récupère une valeur statistique sous forme de chaîne
     */
    private String getStatValueAsString(MineStats stats, StatsValueType type) {
        switch (type) {
            case BLOCKS_MINED: return String.valueOf(stats.getBlocksMined());
            case TOTAL_BLOCKS: return String.valueOf(stats.getTotalBlocks());
            case PERCENTAGE_MINED: return String.valueOf(stats.getPercentageMined());
            case VISITS: return String.valueOf(stats.getVisits());
            default: return "N/A";
        }
    }
} 