package fr.ju.privateMines.managers;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockType;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public class MineResetService {
    private final PrivateMines plugin;

    public MineResetService(PrivateMines plugin) {
        this.plugin = plugin;
    }

    /**
     * Reset une mine par UUID (utilise par l'auto-reset et les commandes admin).
     * Pas de teleportation ni de titre.
     */
    public void resetMine(UUID uuid, MineManager mineManager, PrivateMines plugin, Map<Integer, Map<Material, Double>> mineTiers) {
        resetMineInternal(uuid, mineManager, plugin, mineTiers, null);
    }

    /**
     * Reset une mine pour un joueur connecte.
     * Affiche un titre de chargement, teleporte apres le reset, et affiche un titre de fin.
     */
    public void resetMine(Player player, MineManager mineManager, PrivateMines plugin, Map<Integer, Map<Material, Double>> mineTiers) {
        if (!mineManager.hasMine(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-mine"));
            return;
        }

        player.sendMessage(plugin.getConfigManager().getMessage("mine-reset"));

        Title loadingTitle = Title.title(
            Component.text(ColorUtil.translateColors("&e⚒ &lReset en cours...")),
            Component.text(ColorUtil.translateColors("&7Veuillez patienter")),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(30000), Duration.ofMillis(1000))
        );
        player.showTitle(loadingTitle);

        resetMineInternal(player.getUniqueId(), mineManager, plugin, mineTiers, () -> {
            // Callback sur le main thread APRES que les blocs sont poses

            // Teleporter le joueur
            boolean teleportOnReset = plugin.getConfigManager().getConfig()
                    .getBoolean("Config.Gameplay.teleport-on-reset", true);
            if (teleportOnReset) {
                Mine mine = mineManager.getMine(player).orElse(null);
                if (mine != null) {
                    Location tpLoc = mineManager.getBetterTeleportLocation(mine);
                    player.teleport(tpLoc);
                }
            }

            // Hologramme
            if (plugin.getHologramManager() != null) {
                Mine mine = mineManager.getMine(player).orElse(null);
                if (mine != null) {
                    plugin.getHologramManager().createOrUpdateHologram(mine);
                }
            }

            // Titre de fin
            Title title = Title.title(
                Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessage("titles.mine-reset.title"))),
                Component.text(ColorUtil.translateColors(plugin.getConfigManager().getMessage("titles.mine-reset.subtitle"))),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
            );
            player.showTitle(title);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        });
    }

    /**
     * Logique interne de reset avec callback optionnel.
     * Le callback est execute sur le main thread une fois le reset termine.
     */
    private void resetMineInternal(UUID uuid, MineManager mineManager, PrivateMines plugin,
                                    Map<Integer, Map<Material, Double>> mineTiers, Runnable onComplete) {
        PrivateMines.debugLog("[Reset Debug] Debut de resetMine pour UUID: " + uuid);

        if (!mineManager.hasMine(uuid)) {
            plugin.getLogger().warning("[Reset Debug] resetMine annule: le joueur n'a pas de mine.");
            return;
        }

        Mine mine = mineManager.getMine(uuid).orElse(null);
        if (mine == null) {
            plugin.getLogger().severe("[Reset Debug] resetMine annule: impossible de recuperer l'objet Mine pour UUID: " + uuid);
            return;
        }

        World world = mine.getLocation().getWorld();
        if (world == null) {
            plugin.getLogger().severe("[Reset Debug] resetMine annule: le monde de la mine est null pour UUID: " + uuid);
            return;
        }

        // Tout le reset s'execute en async (FAWE est thread-safe)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Material, Double> mineBlocks = resolveMineBlocks(mine, mineTiers, plugin);
            if (mineBlocks == null || mineBlocks.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    handleCriticalBlockError(plugin, uuid, mine, mineTiers);
                });
                return;
            }

            if (mine.hasMineArea()) {
                // Reset les stats puis remplir les blocs (tout en async)
                mine.calculateTotalBlocks();
                mine.reset();
                fillMineBlocks(mine, mineBlocks, world, plugin);
            } else {
                plugin.getLogger().warning("[Reset Debug] La mine n'a pas de zone definie (hasMineArea=false).");
            }

            // Callback sur le main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                updateStatsAndHolograms(plugin, mineManager, uuid, mine);
                PrivateMines.debugLog("[Reset Debug] Fin de resetMine pour UUID: " + uuid);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    /**
     * Remplit la zone de mine avec les minerais via FAWE EditSession.
     * DOIT etre appele depuis un thread async.
     */
    private void fillMineBlocks(Mine mine, Map<Material, Double> mineBlocks, World world, PrivateMines plugin) {
        PrivateMines.debugLog("[Reset Debug] Remplissage FAWE: (" + mine.getMinX() + "," + mine.getMinY() + "," + mine.getMinZ() +
                ") a (" + mine.getMaxX() + "," + mine.getMaxY() + "," + mine.getMaxZ() + ")");

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            RandomPattern pattern = new RandomPattern();
            for (Map.Entry<Material, Double> entry : mineBlocks.entrySet()) {
                BlockType blockType = BukkitAdapter.asBlockType(entry.getKey());
                if (blockType != null) {
                    pattern.add(blockType.getDefaultState(), entry.getValue());
                }
            }

            CuboidRegion region = new CuboidRegion(
                weWorld,
                BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ()),
                BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ())
            );

            int blocksChanged = editSession.setBlocks(region, pattern);
            PrivateMines.debugLog("[Reset Debug] Mine reset avec FAWE. " + blocksChanged + " blocs modifies.");
        } catch (Exception e) {
            plugin.getLogger().severe("[Reset Debug] Erreur FAWE lors du reset: " + e.getMessage());
            plugin.getErrorHandler().logError("Erreur FAWE lors du reset", e);
        }
    }

    private Map<Material, Double> resolveMineBlocks(Mine mine, Map<Integer, Map<Material, Double>> mineTiers, PrivateMines plugin) {
        int tier = mine.getTier();
        PrivateMines.debugLog("[Reset Debug] Palier (tier) de la mine: " + tier);
        Map<Material, Double> mineBlocks = null;
        if (mineTiers != null && mineTiers.containsKey(tier)) {
            mineBlocks = mineTiers.get(tier);
            PrivateMines.debugLog("[Reset Debug] Utilisation des blocs du palier " + tier + ": " + (mineBlocks != null ? mineBlocks.toString() : "null"));
            if (mineBlocks == null || mineBlocks.isEmpty()) {
                plugin.getLogger().warning("[Reset Debug] Les blocs pour le palier " + tier + " sont nuls ou vides ! Verification de la configuration.");
                mineBlocks = mine.getBlocks();
            }
        } else {
            mineBlocks = mine.getBlocks();
            plugin.getLogger().warning("[Reset Debug] Palier " + tier + " non trouve dans mineTiers. Utilisation des blocs par defaut.");
        }
        return mineBlocks;
    }

    private void handleCriticalBlockError(PrivateMines plugin, UUID uuid, Mine mine, Map<Integer, Map<Material, Double>> mineTiers) {
        plugin.getLogger().severe("[Reset Debug] Echec critique: impossible de determiner les blocs (tier: " + mine.getTier() + ").");
        Player owner = plugin.getServer().getPlayer(uuid);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(plugin.getConfigManager().getMessageOrDefault("reset.critical-error",
                    "&cErreur critique lors de la reinitialisation. Contactez un administrateur."));
        }
    }

    private void updateStatsAndHolograms(PrivateMines plugin, MineManager mineManager, UUID uuid, Mine mine) {
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().onMineReset(mine);
            PrivateMines.debugLog("[Reset Debug] StatsManager notifie de la reinitialisation.");
            mine.synchronizeStats();
        }
        if (plugin.getHologramManager() != null) {
            Mine mineHolo = mineManager.getMine(uuid).orElse(null);
            if (mineHolo != null) {
                plugin.getHologramManager().createOrUpdateHologram(mineHolo);
            }
        }
    }
}
