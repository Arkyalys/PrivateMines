package fr.ju.privateMines.managers;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
public class MineResetService {
    public MineResetService(PrivateMines plugin) {
    }
    public void resetMine(UUID uuid, MineManager mineManager, PrivateMines plugin, Map<Integer, Map<Material, Double>> mineTiers) {
        PrivateMines.debugLog("[Reset Debug] Début de resetMine pour UUID: " + uuid);
        if (!mineManager.hasMine(uuid)) {
            plugin.getLogger().warning("[Reset Debug] resetMine annulé: le joueur n'a pas de mine.");
            return;
        }
        Mine mine = mineManager.getMine(uuid).orElse(null);
        if (mine == null) {
            plugin.getLogger().severe("[Reset Debug] resetMine annulé: impossible de récupérer l'objet Mine pour UUID: " + uuid);
            return;
        }
        World world = mine.getLocation().getWorld();
        if (world == null) {
            plugin.getLogger().severe("[Reset Debug] resetMine annulé: le monde de la mine est null pour UUID: " + uuid);
            return;
        }
        Map<Material, Double> mineBlocks = resolveMineBlocks(mine, mineTiers, plugin);
        if (mineBlocks == null || mineBlocks.isEmpty()) {
            handleCriticalBlockError(plugin, uuid, mine, mineTiers);
            return;
        }
        if (mine.hasMineArea()) {
            handleMineAreaReset(mine, mineBlocks, world, plugin);
        } else {
            plugin.getLogger().warning("[Reset Debug] La mine n'a pas de zone définie (hasMineArea=false). La régénération des blocs est sautée.");
        }
        updateStatsAndHolograms(plugin, mineManager, uuid, mine);
        PrivateMines.debugLog("[Reset Debug] Fin de resetMine pour UUID: " + uuid);
    }

    private Map<Material, Double> resolveMineBlocks(Mine mine, Map<Integer, Map<Material, Double>> mineTiers, PrivateMines plugin) {
        int tier = mine.getTier();
        PrivateMines.debugLog("[Reset Debug] Palier (tier) de la mine: " + tier);
        Map<Material, Double> mineBlocks = null;
        if (mineTiers != null && mineTiers.containsKey(tier)) {
            mineBlocks = mineTiers.get(tier);
            PrivateMines.debugLog("[Reset Debug] Utilisation des blocs du palier " + tier + ": " + (mineBlocks != null ? mineBlocks.toString() : "null"));
            if (mineBlocks == null || mineBlocks.isEmpty()) {
                plugin.getLogger().warning("[Reset Debug] Les blocs pour le palier " + tier + " sont nuls ou vides, même si la clé existe ! Vérifiez la configuration.");
                mineBlocks = mine.getBlocks();
                plugin.getLogger().warning("[Reset Debug] Tentative d'utilisation des blocs par défaut de la mine (type: " + mine.getType() + "): " + (mineBlocks != null ? mineBlocks.toString() : "null"));
            }
        } else {
            mineBlocks = mine.getBlocks();
            plugin.getLogger().warning("[Reset Debug] Palier " + tier + " non trouvé dans mineTiers. Utilisation des blocs par défaut de la mine (type: " + mine.getType() + "): " + (mineBlocks != null ? mineBlocks.toString() : "null"));
        }
        return mineBlocks;
    }

    private void handleCriticalBlockError(PrivateMines plugin, UUID uuid, Mine mine, Map<Integer, Map<Material, Double>> mineTiers) {
        plugin.getLogger().severe("[Reset Debug] Échec critique: impossible de déterminer les blocs pour la régénération (tier: " + mine.getTier() + ", type: " + mine.getType() + "). La régénération est annulée.");
        Player owner = plugin.getServer().getPlayer(uuid);
        if(owner != null && owner.isOnline()) {
            owner.sendMessage(ColorUtil.translateColors("&cErreur critique lors de la réinitialisation : impossible de trouver les blocs à placer. Contactez un administrateur."));
        }
    }

    private void handleMineAreaReset(Mine mine, Map<Material, Double> mineBlocks, World world, PrivateMines plugin) {
        PrivateMines.debugLog("[Reset Debug] La mine a une zone définie (hasMineArea=true).");
        mine.calculateTotalBlocks();
        PrivateMines.debugLog("[Reset Debug] Nombre total de blocs calculé: " + mine.getStats().getTotalBlocks());
        mine.reset();
        PrivateMines.debugLog("[Reset Debug] Statistiques de la mine réinitialisées (mine.reset() appelé).");
        try {
            PrivateMines.debugLog("[Reset Debug] Utilisation de FAWE pour réinitialiser la mine: " + "(" + mine.getMinX() + "," + mine.getMinY() + "," + mine.getMinZ() + ") à " + "(" + mine.getMaxX() + "," + mine.getMaxY() + "," + mine.getMaxZ() + ")");
            com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);
            com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld);
            com.sk89q.worldedit.function.pattern.RandomPattern pattern = new com.sk89q.worldedit.function.pattern.RandomPattern();
            for (Map.Entry<Material, Double> entry : mineBlocks.entrySet()) {
                com.sk89q.worldedit.world.block.BlockType blockType = com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockType(entry.getKey());
                if (blockType != null) {
                    pattern.add(blockType.getDefaultState(), entry.getValue() / 100.0);
                }
            }
            com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(
                weWorld,
                com.sk89q.worldedit.math.BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ()),
                com.sk89q.worldedit.math.BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ())
            );
            int blocksChanged = editSession.setBlocks(region, pattern);
            editSession.close();
            PrivateMines.debugLog("[Reset Debug] Mine réinitialisée avec FAWE. " + blocksChanged + " blocs modifiés.");
        } catch (Exception e) {
            plugin.getLogger().severe("[Reset Debug] Erreur critique lors de la réinitialisation avec FAWE: " + e.getMessage());
            e.printStackTrace();
            plugin.getLogger().severe("FAWE est maintenant obligatoire, vérifiez votre installation.");
        }
    }

    private void updateStatsAndHolograms(PrivateMines plugin, MineManager mineManager, UUID uuid, Mine mine) {
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().onMineReset(mine);
            PrivateMines.debugLog("[Reset Debug] StatsManager notifié de la réinitialisation.");
            mine.synchronizeStats();
            PrivateMines.debugLog("[Reset Debug] Synchronisation des statistiques effectuée après reset");
        }
        if (plugin.getHologramManager() != null) {
            Mine mineHolo = mineManager.getMine(uuid).orElse(null);
            if (mineHolo != null) {
                plugin.getHologramManager().createOrUpdateHologram(mineHolo);
            }
        }
    }

    public void resetMine(Player player, MineManager mineManager, PrivateMines plugin, Map<Integer, Map<Material, Double>> mineTiers) {
        if (!mineManager.hasMine(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("Messages.no-mine"));
            return;
        }
        player.sendMessage(plugin.getConfigManager().getMessage("Messages.mine-reset"));
        resetMine(player.getUniqueId(), mineManager, plugin, mineTiers);
        if (plugin.getHologramManager() != null) {
            Mine mine = mineManager.getMine(player).orElse(null);
            if (mine != null) {
                plugin.getHologramManager().createOrUpdateHologram(mine);
            }
        }
        Title title = Title.title(
            Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessage("Messages.titles.mine-reset.title"))),
            Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessage("Messages.titles.mine-reset.subtitle"))),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );
        player.showTitle(title);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }
} 