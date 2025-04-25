package fr.ju.privateMines.utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class MineAreaDetector {
    public MineAreaDetector(PrivateMines plugin) {
    }
    public boolean detectMineArea(Mine mine) {
        Location center = mine.getLocation();
        Location topRail = null;
        Location bottomRail = null;
        PrivateMines.debugLog("Starting marker detection for mine at " + center.toString());
        int searchRadius = 150;
        PrivateMines.debugLog("Searching for rails in a radius of " + searchRadius + " blocks...");
        PrivateMines.debugLog("Searching for powered rails...");
        Location[] railLocations = searchForMarkers(center, searchRadius, Material.POWERED_RAIL, "powered rail");
        if (railLocations != null) {
            topRail = railLocations[0];
            bottomRail = railLocations[1];
            return processFoundMarkers(mine, topRail, bottomRail);
        }
        PrivateMines.debugLog("No powered rail found, searching for regular rails...");
        railLocations = searchForMarkers(center, searchRadius, Material.RAIL, "regular rail");
        if (railLocations != null) {
            topRail = railLocations[0];
            bottomRail = railLocations[1];
            return processFoundMarkers(mine, topRail, bottomRail);
        }
        PrivateMines.debugLog("No regular rail found, searching for detector rails...");
        railLocations = searchForMarkers(center, searchRadius, Material.DETECTOR_RAIL, "detector rail");
        if (railLocations != null) {
            topRail = railLocations[0];
            bottomRail = railLocations[1];
            return processFoundMarkers(mine, topRail, bottomRail);
        }
        PrivateMines.debugLog("No detector rail found, searching for redstone blocks...");
        railLocations = searchForMarkers(center, searchRadius, Material.REDSTONE_BLOCK, "redstone block");
        if (railLocations != null) {
            topRail = railLocations[0];
            bottomRail = railLocations[1];
            return processFoundMarkers(mine, topRail, bottomRail);
        }
        PrivateMines.debugLog("No marker found in the mine after thorough search!");
        return false;
    }
    private Location[] searchForMarkers(Location center, int searchRadius,
                                      Material markerType, String markerName) {
        boolean found = false;
        List<Location> allRailLocations = new ArrayList<>();
        for (int y = searchRadius; y >= -searchRadius; y--) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == markerType) {
                        allRailLocations.add(loc);
                        found = true;
                    }
                }
            }
        }
        if (found && !allRailLocations.isEmpty()) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            double maxZ = Double.MIN_VALUE;
            for (Location railLoc : allRailLocations) {
                minX = Math.min(minX, railLoc.getBlockX());
                minY = Math.min(minY, railLoc.getBlockY());
                minZ = Math.min(minZ, railLoc.getBlockZ());
                maxX = Math.max(maxX, railLoc.getBlockX());
                maxY = Math.max(maxY, railLoc.getBlockY());
                maxZ = Math.max(maxZ, railLoc.getBlockZ());
            }
            Location minLocation = new Location(center.getWorld(), minX, minY, minZ);
            Location maxLocation = new Location(center.getWorld(), maxX, maxY, maxZ);
            return new Location[] { maxLocation, minLocation };
        }
        return null;
    }
    private boolean processFoundMarkers(Mine mine, Location topRail, Location bottomRail) {
        if (topRail == null || bottomRail == null) {
            PrivateMines.debugLog("Incomplete markers!");
            return false;
        }
        int minX = Math.min(topRail.getBlockX(), bottomRail.getBlockX());
        int maxX = Math.max(topRail.getBlockX(), bottomRail.getBlockX());
        int minY = Math.min(topRail.getBlockY(), bottomRail.getBlockY());
        int maxY = Math.max(topRail.getBlockY(), bottomRail.getBlockY());
        int minZ = Math.min(topRail.getBlockZ(), bottomRail.getBlockZ());
        int maxZ = Math.max(topRail.getBlockZ(), bottomRail.getBlockZ());
        mine.setMineArea(minX, minY, minZ, maxX, maxY, maxZ);
        PrivateMines.debugLog("[DEBUG] Mine area set, totalBlocks=" + 
                              mine.getStats().getTotalBlocks() + ", blocksMined=" + 
                              mine.getStats().getBlocksMined());
        fillMineWithOres(mine, minX, minY, minZ, maxX, maxY, maxZ);
        return true;
    }
    private void fillMineWithOres(Mine mine, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Map<Material, Double> blockDistribution = getBlockDistribution(mine);
        List<Material> weightedMaterials = buildWeightedMaterials(blockDistribution);
        fillBlocksInArea(mine, minX, minY, minZ, maxX, maxY, maxZ, weightedMaterials);
    }
    private Map<Material, Double> getBlockDistribution(Mine mine) {
        Map<Material, Double> blockDistribution = mine.getBlocks();
        if (blockDistribution == null || blockDistribution.isEmpty()) {
            blockDistribution = new HashMap<>();
            blockDistribution.put(Material.STONE, 0.7);
            blockDistribution.put(Material.COAL_ORE, 0.15);
            blockDistribution.put(Material.IRON_ORE, 0.1);
            blockDistribution.put(Material.GOLD_ORE, 0.05);
        }
        return blockDistribution;
    }
    private List<Material> buildWeightedMaterials(Map<Material, Double> blockDistribution) {
        List<Material> weightedMaterials = new ArrayList<>();
        for (Map.Entry<Material, Double> entry : blockDistribution.entrySet()) {
            int weight = (int) (entry.getValue() * 100);
            for (int i = 0; i < weight; i++) {
                weightedMaterials.add(entry.getKey());
            }
        }
        if (weightedMaterials.isEmpty()) {
            weightedMaterials.add(Material.STONE);
        }
        return weightedMaterials;
    }
    private void fillBlocksInArea(Mine mine, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, List<Material> weightedMaterials) {
        Random random = new Random();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = mine.getLocation().getWorld().getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) {
                        Material material = weightedMaterials.get(random.nextInt(weightedMaterials.size()));
                        block.setType(material);
                    }
                }
            }
        }
    }
    public void helpPlaceRails(Player player) {
        player.sendMessage(ColorUtil.translateColors("&eTo define the ore spawn area:"));
        player.sendMessage(ColorUtil.translateColors("&7- Place a &6powered rail &7at the top of the area"));
        player.sendMessage(ColorUtil.translateColors("&7- Place a &6powered rail &7at the bottom of the area"));
        player.sendMessage(ColorUtil.translateColors("&7- The rails must be placed at opposite corners of the area"));
        player.sendMessage(ColorUtil.translateColors("&7- Once the rails are placed, use &6/jumine create &7to create the mine"));
    }
} 