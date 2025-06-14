package fr.ju.privateMines.managers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.services.HologramCreationService;
import fr.ju.privateMines.services.HologramDeleteService;
import fr.ju.privateMines.services.HologramUpdateService;
import fr.ju.privateMines.utils.ProgressBarUtil;
public class HologramManager {
    private final PrivateMines plugin;
    public final Map<UUID, Map<String, String>> mineHolograms; 
    private final HologramCreationService hologramCreationService;
    private final HologramDeleteService hologramDeleteService;
    private final HologramUpdateService hologramUpdateService;
    public HologramManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineHolograms = new HashMap<>();
        this.hologramCreationService = new HologramCreationService(plugin, this);
        this.hologramDeleteService = new HologramDeleteService(plugin, this);
        this.hologramUpdateService = new HologramUpdateService(plugin, this);
    }
    public void createOrUpdateHologram(Mine mine) {
        if (mine == null || !mine.hasMineArea()) {
            return;
        }
        if (!mineHolograms.containsKey(mine.getOwner())) {
            mineHolograms.put(mine.getOwner(), new HashMap<>());
        }
        int minX = mine.getMinX();
        int maxX = mine.getMaxX();
        int minZ = mine.getMinZ();
        int maxZ = mine.getMaxZ();
        int topY = mine.getMaxY() + 5; 
        Location[] cornerLocations = {
            new Location(mine.getLocation().getWorld(), minX + 0.5, topY, minZ + 0.5), 
            new Location(mine.getLocation().getWorld(), maxX + 0.5, topY, minZ + 0.5), 
            new Location(mine.getLocation().getWorld(), minX + 0.5, topY, maxZ + 0.5), 
            new Location(mine.getLocation().getWorld(), maxX + 0.5, topY, maxZ + 0.5)  
        };
        String[] cornerNames = {"nw", "ne", "sw", "se"};
        List<String> resourceLines = generateResourceHologramLines(mine);
        List<String> minerLines = generateAutoMinerHologramLines(mine);
        List<String> playerInfoLines = generatePlayerInfoHologramLines(mine);
        List<List<String>> cornerContents = new ArrayList<>();
        cornerContents.add(resourceLines);     
        cornerContents.add(minerLines);        
        cornerContents.add(playerInfoLines);   
        List<String> emptyCornerLines = new ArrayList<>();
        emptyCornerLines.add("&6&l✦ &f&lMine Privée &6&l✦");
        emptyCornerLines.add("");
        emptyCornerLines.add("&e&lBienvenue !");
        cornerContents.add(emptyCornerLines);  
        for (int i = 0; i < cornerLocations.length; i++) {
            String cornerName = cornerNames[i];
            String holoName = "mine-" + mine.getOwner().toString() + "-" + cornerName;
            Location holoLocation = cornerLocations[i];
            createOrUpdateSingleHologram(mine, holoName, holoLocation, cornerContents.get(i));
            mineHolograms.get(mine.getOwner()).put(cornerName, holoName);
        }
        String centralHoloName = "mine-" + mine.getOwner().toString() + "-center";
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        Location centralHoloLocation = new Location(mine.getLocation().getWorld(), centerX + 0.5, 70, centerZ + 0.5);
        List<String> centralHoloLines = generateCentralHologramLines(mine);
        createOrUpdateSingleHologram(mine, centralHoloName, centralHoloLocation, centralHoloLines);
        mineHolograms.get(mine.getOwner()).put("center", centralHoloName);
    }
    private void createOrUpdateSingleHologram(Mine mine, String holoName, Location holoLocation, List<String> holoLines) {
        hologramCreationService.createOrUpdateSingleHologram(mine, holoName, holoLocation, holoLines);
    }
    public void removeHologram(UUID ownerId) {
        hologramDeleteService.removeHologram(ownerId);
    }
    public void updateAllHolograms() {
        hologramUpdateService.updateAllHolograms();
    }
    private List<String> generateCentralHologramLines(Mine mine) {
        MineStats stats = mine.getStats();
        String ownerName = plugin.getServer().getOfflinePlayer(mine.getOwner()).getName();
        if (ownerName == null) {
            ownerName = "Inconnu";
        }
        int resetThreshold = plugin.getConfigManager().getConfig().getInt("Gameplay.auto-reset.threshold", 65);
        int percentageMined = stats.getPercentageMined();
        String progressBar = ProgressBarUtil.createProgressBar(percentageMined);
        List<String> allLines = new java.util.ArrayList<>();
        allLines.add("&6&l✦ &f&lMine Privée &6&l✦");
        allLines.add("");
        allLines.add("&e&lPropriétaire: &f" + ownerName);
        allLines.add("&e&lStatut: " + (mine.isOpen() ? "&a&lOUVERTE" : "&c&lFERMÉE"));
        allLines.add("");
        allLines.add("&f&lProgression: &f" + percentageMined + "% &7(" + stats.getBlocksMined() + "/" + stats.getTotalBlocks() + ")");
        allLines.add("&f" + progressBar);
        String resetInfo = "&7Auto-reset à " + resetThreshold + "%";
        allLines.add(resetInfo);
        allLines.add("");
        allLines.add("&f&lInformations:");
        allLines.add("&7➤ &fVisites: &e" + stats.getVisits());
        allLines.add("&7➤ &fTaxe: &e" + mine.getTax() + "%");
        return allLines;
    }
    private List<String> generateResourceHologramLines(Mine mine) {
        List<String> allLines = new java.util.ArrayList<>();
        allLines.add("&6&l✦ &f&lRessources &6&l✦");
        allLines.add("");
        Map<org.bukkit.Material, Double> resources = getMineResources(mine);
        if (resources.isEmpty()) {
            allLines.add("&cAucune ressource définie");
        } else {
            List<Map.Entry<org.bukkit.Material, Double>> sortedResources = new java.util.ArrayList<>(resources.entrySet());
            sortedResources.sort(Map.Entry.<org.bukkit.Material, Double>comparingByValue().reversed());
            for (Map.Entry<org.bukkit.Material, Double> entry : sortedResources) {
                String materialName = formatMaterialName(entry.getKey());
                double percentage = entry.getValue();
                String colorCode = "&f"; 
                if (percentage < 5) {
                    colorCode = "&d"; 
                } else if (percentage < 15) {
                    colorCode = "&b"; 
                } else if (percentage < 30) {
                    colorCode = "&a"; 
                } else {
                    colorCode = "&7"; 
                }
                String iconLine = "#ICON: " + entry.getKey().toString() + " " + colorCode + materialName + ": &e" + String.format("%.1f", percentage) + "%";
                allLines.add(iconLine);
            }
        }
        return allLines;
    }
    private Map<org.bukkit.Material, Double> getMineResources(Mine mine) {
        return mine.getBlocks();
    }
    private String formatMaterialName(org.bukkit.Material material) {
        String name = material.toString();
        name = name.toLowerCase().replace('_', ' ');
        StringBuilder formattedName = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formattedName.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formattedName.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formattedName.append(c);
            }
        }
        return formattedName.toString();
    }
    private List<String> generateAutoMinerHologramLines(Mine mine) {
        List<String> allLines = new java.util.ArrayList<>();
        allLines.add("&6&l✦ &f&lAuto Miner &6&l✦");
        allLines.add("");
        allLines.add("&e&lStatistiques:");
        int autoMinersCount = 0; 
        int autoMinerEfficiency = 100; 
        allLines.add("#ICON: IRON_PICKAXE &7Nombre: &e" + autoMinersCount);
        allLines.add("#ICON: EXPERIENCE_BOTTLE &7Efficacité: &e" + autoMinerEfficiency + "%");
        allLines.add("#ICON: GOLD_INGOT &7Gains: &e$0/h");
        allLines.add("");
        allLines.add("&7Pour acheter un Auto Miner:");
        allLines.add("&7/mine autominer buy");
        return allLines;
    }
    private List<String> generatePlayerInfoHologramLines(Mine mine) {
        List<String> allLines = new java.util.ArrayList<>();
        
        // En-tête
        addHeaderLines(allLines);
        
        // Informations du propriétaire
        addOwnerInfo(allLines, mine);
        
        // Statistiques de la mine
        addMineStats(allLines, mine);
        
        // Statut d'ouverture
        addMineStatus(allLines, mine);
        
        // Informations sur le dernier reset
        addLastResetInfo(allLines, mine);
        
        return allLines;
    }
    
    /**
     * Ajoute l'en-tête du hologramme d'informations joueur
     */
    private void addHeaderLines(List<String> lines) {
        lines.add("&6&l✦ &f&lInformations Joueur &6&l✦");
        lines.add("");
    }
    
    /**
     * Ajoute les informations du propriétaire de la mine
     */
    private void addOwnerInfo(List<String> lines, Mine mine) {
        String ownerName = plugin.getServer().getOfflinePlayer(mine.getOwner()).getName();
        if (ownerName == null) {
            ownerName = "Inconnu";
        }
        lines.add("&e&lPropriétaire: &f" + ownerName);
    }
    
    /**
     * Ajoute les statistiques principales de la mine
     */
    private void addMineStats(List<String> lines, Mine mine) {
        MineStats stats = mine.getStats();
        lines.add("#ICON: DIAMOND &7Niveau: &e" + mine.getTier());
        lines.add("#ICON: COMPASS &7Visites: &e" + stats.getVisits());
        lines.add("#ICON: GOLD_NUGGET &7Taxe: &e" + mine.getTax() + "%");
    }
    
    /**
     * Ajoute l'information sur le statut d'ouverture de la mine
     */
    private void addMineStatus(List<String> lines, Mine mine) {
        String statusIcon = mine.isOpen() ? "LIME_CONCRETE" : "RED_CONCRETE";
        lines.add("#ICON: " + statusIcon + " &7Statut: " + (mine.isOpen() ? "&aOuverte" : "&cFermée"));
    }
    
    /**
     * Ajoute l'information sur le dernier reset de la mine
     */
    private void addLastResetInfo(List<String> lines, Mine mine) {
        MineStats stats = mine.getStats();
        long lastResetMillis = stats.getLastReset();
        String timeAgo = formatTimeSinceReset(lastResetMillis);
        lines.add("#ICON: CLOCK &7Dernier reset: &e" + timeAgo);
    }
    
    /**
     * Formate le temps écoulé depuis le dernier reset en chaîne lisible
     */
    private String formatTimeSinceReset(long lastResetMillis) {
        long currentMillis = System.currentTimeMillis();
        long diffMillis = currentMillis - lastResetMillis;
        long diffMinutes = diffMillis / (60 * 1000);
        
        if (diffMinutes < 60) {
            return formatTimeInMinutes(diffMinutes);
        } else {
            long diffHours = diffMinutes / 60;
            if (diffHours < 24) {
                return formatTimeInHours(diffHours);
            } else {
                return formatTimeInDays(diffHours / 24);
            }
        }
    }
    
    /**
     * Formate un temps en minutes avec pluriel approprié
     */
    private String formatTimeInMinutes(long minutes) {
        return minutes + " minute" + (minutes > 1 ? "s" : "");
    }
    
    /**
     * Formate un temps en heures avec pluriel approprié
     */
    private String formatTimeInHours(long hours) {
        return hours + " heure" + (hours > 1 ? "s" : "");
    }
    
    /**
     * Formate un temps en jours avec pluriel approprié
     */
    private String formatTimeInDays(long days) {
        return days + " jour" + (days > 1 ? "s" : "");
    }
}