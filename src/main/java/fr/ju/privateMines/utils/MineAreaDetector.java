package fr.ju.privateMines.utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class MineAreaDetector {
    private static final Material[] MARKER_TYPES = {
        Material.POWERED_RAIL,
        Material.RAIL,
        Material.DETECTOR_RAIL,
        Material.REDSTONE_BLOCK
    };
    private static final String[] MARKER_NAMES = {
        "powered rail",
        "regular rail",
        "detector rail",
        "redstone block"
    };

    public MineAreaDetector(PrivateMines plugin) {
    }

    public boolean detectMineArea(Mine mine) {
        Location center = mine.getLocation();
        PrivateMines.debugLog("Début de la détection de marqueurs pour la mine à " + center.toString());

        // Recherche progressive : d'abord petit rayon, puis plus grand si nécessaire
        int[] searchRadii = {50, 100, 150};

        for (int radius : searchRadii) {
            PrivateMines.debugLog("Recherche avec rayon de " + radius + " blocs...");
            for (int i = 0; i < MARKER_TYPES.length; i++) {
                PrivateMines.debugLog("Recherche de " + MARKER_NAMES[i] + "...");
                Location[] railLocations = searchForMarkersChunkBased(center, radius, MARKER_TYPES[i]);
                if (railLocations != null) {
                    return processFoundMarkers(mine, railLocations[0], railLocations[1]);
                }
            }
        }

        PrivateMines.debugLog("Aucun marqueur trouvé après recherche complète !");
        return false;
    }

    /**
     * Recherche optimisée par chunks au lieu de bloc par bloc.
     * Au lieu d'itérer sur radius³ blocs individuels (27M pour radius=150),
     * on itère sur les chunks concernés et utilise ChunkSnapshot pour un accès rapide.
     */
    private Location[] searchForMarkersChunkBased(Location center, int searchRadius,
                                                   Material markerType) {
        World world = center.getWorld();
        if (world == null) return null;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        int minChunkX = (centerX - searchRadius) >> 4;
        int maxChunkX = (centerX + searchRadius) >> 4;
        int minChunkZ = (centerZ - searchRadius) >> 4;
        int maxChunkZ = (centerZ + searchRadius) >> 4;

        int minY = Math.max(world.getMinHeight(), centerY - searchRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + searchRadius);

        int foundMinX = Integer.MAX_VALUE, foundMinY = Integer.MAX_VALUE, foundMinZ = Integer.MAX_VALUE;
        int foundMaxX = Integer.MIN_VALUE, foundMaxY = Integer.MIN_VALUE, foundMaxZ = Integer.MIN_VALUE;
        boolean found = false;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    world.getChunkAt(cx, cz);
                }

                ChunkSnapshot snapshot = world.getChunkAt(cx, cz).getChunkSnapshot();
                int chunkBaseX = cx << 4;
                int chunkBaseZ = cz << 4;

                // Limiter la recherche aux blocs dans le rayon
                int blockMinX = Math.max(0, (centerX - searchRadius) - chunkBaseX);
                int blockMaxX = Math.min(15, (centerX + searchRadius) - chunkBaseX);
                int blockMinZ = Math.max(0, (centerZ - searchRadius) - chunkBaseZ);
                int blockMaxZ = Math.min(15, (centerZ + searchRadius) - chunkBaseZ);

                for (int y = maxY; y >= minY; y--) {
                    for (int bx = blockMinX; bx <= blockMaxX; bx++) {
                        for (int bz = blockMinZ; bz <= blockMaxZ; bz++) {
                            Material type = snapshot.getBlockType(bx, y, bz);
                            if (type == markerType) {
                                int worldX = chunkBaseX + bx;
                                int worldZ = chunkBaseZ + bz;
                                foundMinX = Math.min(foundMinX, worldX);
                                foundMinY = Math.min(foundMinY, y);
                                foundMinZ = Math.min(foundMinZ, worldZ);
                                foundMaxX = Math.max(foundMaxX, worldX);
                                foundMaxY = Math.max(foundMaxY, y);
                                foundMaxZ = Math.max(foundMaxZ, worldZ);
                                found = true;
                            }
                        }
                    }
                }
            }
        }

        if (found) {
            Location maxLocation = new Location(world, foundMaxX, foundMaxY, foundMaxZ);
            Location minLocation = new Location(world, foundMinX, foundMinY, foundMinZ);
            PrivateMines.debugLog("Marqueurs trouvés : min=(" + foundMinX + "," + foundMinY + "," + foundMinZ +
                                  ") max=(" + foundMaxX + "," + foundMaxY + "," + foundMaxZ + ")");
            return new Location[] { maxLocation, minLocation };
        }

        return null;
    }

    private boolean processFoundMarkers(Mine mine, Location topRail, Location bottomRail) {
        if (topRail == null || bottomRail == null) {
            PrivateMines.debugLog("Marqueurs incomplets !");
            return false;
        }
        int minX = Math.min(topRail.getBlockX(), bottomRail.getBlockX());
        int maxX = Math.max(topRail.getBlockX(), bottomRail.getBlockX());
        int minY = Math.min(topRail.getBlockY(), bottomRail.getBlockY());
        int maxY = Math.max(topRail.getBlockY(), bottomRail.getBlockY());
        int minZ = Math.min(topRail.getBlockZ(), bottomRail.getBlockZ());
        int maxZ = Math.max(topRail.getBlockZ(), bottomRail.getBlockZ());
        mine.setMineArea(minX, minY, minZ, maxX, maxY, maxZ);
        PrivateMines.debugLog("[DEBUG] Zone de mine définie, totalBlocks=" +
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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = mine.getLocation().getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) {
                        Material material = weightedMaterials.get(random.nextInt(weightedMaterials.size()));
                        block.setType(material);
                    }
                }
            }
        }
    }

    public void helpPlaceRails(Player player) {
        player.sendMessage(ColorUtil.translateColors("&ePour définir la zone de spawn des minerais :"));
        player.sendMessage(ColorUtil.translateColors("&7- Placez un &6rail alimenté &7en haut de la zone"));
        player.sendMessage(ColorUtil.translateColors("&7- Placez un &6rail alimenté &7en bas de la zone"));
        player.sendMessage(ColorUtil.translateColors("&7- Les rails doivent être placés aux coins opposés de la zone"));
        player.sendMessage(ColorUtil.translateColors("&7- Une fois les rails placés, utilisez &6/jumine create &7pour créer la mine"));
    }
}
