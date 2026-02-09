package fr.ju.privateMines.commands;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

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
        int maxSize = configManager.getMaxMineSize();
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

        // Remplissage async via FAWE puis callback sync pour save/reset
        fillMineWithOresAndBedrockAsync(mine, () -> {
            mineManager.saveMineData(player);
            mineManager.resetMine(player);
            player.sendMessage(configManager.getMessage("mine-expanded").replace("%size%", String.valueOf(mine.getSize())));
        });
        return true;
    }

    private void expandMineArea(Mine mine) {
        mine.setMineArea(
            mine.getMinX() - 1,
            mine.getMinY(),
            mine.getMinZ() - 1,
            mine.getMaxX() + 1,
            mine.getMaxY(),
            mine.getMaxZ() + 1
        );
    }

    private void fillMineWithOresAndBedrockAsync(Mine mine, Runnable onComplete) {
        org.bukkit.World world = mine.getLocation().getWorld();
        if (world == null || !mine.hasMineArea()) {
            onComplete.run();
            return;
        }

        int minX = mine.getMinX();
        int maxX = mine.getMaxX();
        int minY = mine.getMinY();
        int maxY = mine.getMaxY();
        int minZ = mine.getMinZ();
        int maxZ = mine.getMaxZ();
        Map<org.bukkit.Material, Double> blockDistribution = getBlockDistribution(mine);
        // Pré-calculer la weighted list sur le thread principal
        org.bukkit.Material[] weightedArray = buildWeightedArray(blockDistribution);

        PrivateMines plugin = PrivateMines.getInstance();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Random random = new Random();

                // Remplir les blocs d'air avec des minerais
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockVector3 pos = BlockVector3.at(x, y, z);
                            if (editSession.getBlock(pos).getBlockType() == BlockTypes.AIR) {
                                org.bukkit.Material mat = weightedArray[random.nextInt(weightedArray.length)];
                                com.sk89q.worldedit.world.block.BlockType weType = BukkitAdapter.asBlockType(mat);
                                if (weType != null) {
                                    editSession.setBlock(pos, weType.getDefaultState());
                                }
                            }
                        }
                    }
                }

                // Murs de bedrock
                com.sk89q.worldedit.world.block.BlockState bedrock = BlockTypes.BEDROCK.getDefaultState();
                for (int y = minY; y <= maxY; y++) {
                    for (int x2 = minX; x2 <= maxX; x2++) {
                        editSession.setBlock(BlockVector3.at(x2, y, minZ - 1), bedrock);
                        editSession.setBlock(BlockVector3.at(x2, y, maxZ + 1), bedrock);
                    }
                    for (int z2 = minZ; z2 <= maxZ; z2++) {
                        editSession.setBlock(BlockVector3.at(minX - 1, y, z2), bedrock);
                        editSession.setBlock(BlockVector3.at(maxX + 1, y, z2), bedrock);
                    }
                }

                // Sol de bedrock
                for (int x3 = minX; x3 <= maxX; x3++) {
                    for (int z3 = minZ; z3 <= maxZ; z3++) {
                        editSession.setBlock(BlockVector3.at(x3, minY - 1, z3), bedrock);
                    }
                }
            } catch (Exception e) {
                PrivateMines.getInstance().getLogger().severe("Erreur FAWE lors de l'expansion: " + e.getMessage());
            }

            // Callback sync sur le main thread
            Bukkit.getScheduler().runTask(plugin, onComplete);
        });
    }

    private Map<org.bukkit.Material, Double> getBlockDistribution(Mine mine) {
        Map<org.bukkit.Material, Double> blockDistribution = mine.getBlocks();
        if (blockDistribution == null || blockDistribution.isEmpty()) {
            blockDistribution = new java.util.HashMap<>();
            blockDistribution.put(org.bukkit.Material.STONE, 0.7);
            blockDistribution.put(org.bukkit.Material.COAL_ORE, 0.15);
            blockDistribution.put(org.bukkit.Material.IRON_ORE, 0.1);
            blockDistribution.put(org.bukkit.Material.GOLD_ORE, 0.05);
        }
        return blockDistribution;
    }

    private org.bukkit.Material[] buildWeightedArray(Map<org.bukkit.Material, Double> blockDistribution) {
        java.util.List<org.bukkit.Material> list = new java.util.ArrayList<>();
        for (Map.Entry<org.bukkit.Material, Double> entry : blockDistribution.entrySet()) {
            int weight = (int) (entry.getValue() * 100);
            for (int i = 0; i < weight; i++) {
                list.add(entry.getKey());
            }
        }
        if (list.isEmpty()) {
            list.add(org.bukkit.Material.STONE);
        }
        return list.toArray(new org.bukkit.Material[0]);
    }
}
