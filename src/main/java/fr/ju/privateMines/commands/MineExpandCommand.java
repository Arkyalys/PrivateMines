package fr.ju.privateMines.commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
import fr.ju.privateMines.PrivateMines;
public class MineExpandCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineExpandCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.EXPAND)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        var mineOpt = mineManager.getMine(player);
        if (mineOpt.isEmpty()) {
            player.sendMessage(configManager.getMessage("mine-no-mine"));
            return true;
        }
        var mine = mineOpt.get();
        int maxSize = configManager.getConfig().getInt("Config.Mines.max-size", 100);
        if (mine.getSize() >= maxSize) {
            player.sendMessage(configManager.getMessage("mine-max-size-reached"));
            return true;
        }
        mine.expand();
        PrivateMines.getInstance().getMetricsService().incrementMineExpansions();
        if (mine.hasMineArea()) {
            expandMineArea(mine);
        }
        mineManager.getMineProtectionManager().updateMineProtection(mine);
        fillMineWithOresAndBedrock(mine);
        mineManager.saveMineData(player);
        mineManager.resetMine(player);
        player.sendMessage(configManager.getMessage("mine-expanded").replace("{size}", String.valueOf(mine.getSize())));
        return true;
    }
    private void expandMineArea(fr.ju.privateMines.models.Mine mine) {
        mine.setMineArea(
            mine.getMinX() - 1,
            mine.getMinY(),
            mine.getMinZ() - 1,
            mine.getMaxX() + 1,
            mine.getMaxY(),
            mine.getMaxZ() + 1
        );
    }
    private void fillMineWithOresAndBedrock(fr.ju.privateMines.models.Mine mine) {
        org.bukkit.World world = mine.getLocation().getWorld();
        if (world != null && mine.hasMineArea()) {
            int minX = mine.getMinX();
            int maxX = mine.getMaxX();
            int minY = mine.getMinY();
            int maxY = mine.getMaxY();
            int minZ = mine.getMinZ();
            int maxZ = mine.getMaxZ();
            java.util.Map<org.bukkit.Material, Double> blockDistribution = getBlockDistribution(mine);
            java.util.List<org.bukkit.Material> weightedMaterials = buildWeightedMaterials(blockDistribution);
            fillAirBlocks(world, minX, maxX, minY, maxY, minZ, maxZ, weightedMaterials);
            placeBedrockWalls(world, minX, maxX, minY, maxY, minZ, maxZ);
            placeBedrockFloor(world, minX, maxX, minY, minZ, maxZ);
        }
    }
    private java.util.Map<org.bukkit.Material, Double> getBlockDistribution(fr.ju.privateMines.models.Mine mine) {
        java.util.Map<org.bukkit.Material, Double> blockDistribution = mine.getBlocks();
        if (blockDistribution == null || blockDistribution.isEmpty()) {
            blockDistribution = new java.util.HashMap<>();
            blockDistribution.put(org.bukkit.Material.STONE, 0.7);
            blockDistribution.put(org.bukkit.Material.COAL_ORE, 0.15);
            blockDistribution.put(org.bukkit.Material.IRON_ORE, 0.1);
            blockDistribution.put(org.bukkit.Material.GOLD_ORE, 0.05);
        }
        return blockDistribution;
    }
    private java.util.List<org.bukkit.Material> buildWeightedMaterials(java.util.Map<org.bukkit.Material, Double> blockDistribution) {
        java.util.List<org.bukkit.Material> weightedMaterials = new java.util.ArrayList<>();
        for (java.util.Map.Entry<org.bukkit.Material, Double> entry : blockDistribution.entrySet()) {
            int weight = (int) (entry.getValue() * 100);
            for (int i = 0; i < weight; i++) {
                weightedMaterials.add(entry.getKey());
            }
        }
        if (weightedMaterials.isEmpty()) {
            weightedMaterials.add(org.bukkit.Material.STONE);
        }
        return weightedMaterials;
    }
    private void fillAirBlocks(org.bukkit.World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, java.util.List<org.bukkit.Material> weightedMaterials) {
        java.util.Random random = new java.util.Random();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == org.bukkit.Material.AIR) {
                        org.bukkit.Material material = weightedMaterials.get(random.nextInt(weightedMaterials.size()));
                        block.setType(material);
                    }
                }
            }
        }
    }
    private void placeBedrockWalls(org.bukkit.World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                world.getBlockAt(x, y, minZ - 1).setType(org.bukkit.Material.BEDROCK);
                world.getBlockAt(x, y, maxZ + 1).setType(org.bukkit.Material.BEDROCK);
            }
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(minX - 1, y, z).setType(org.bukkit.Material.BEDROCK);
                world.getBlockAt(maxX + 1, y, z).setType(org.bukkit.Material.BEDROCK);
            }
        }
    }
    private void placeBedrockFloor(org.bukkit.World world, int minX, int maxX, int minY, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, minY - 1, z).setType(org.bukkit.Material.BEDROCK);
            }
        }
    }
} 