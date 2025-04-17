package fr.ju.privateMines.managers;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
public class MinePregenService {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    public MinePregenService(PrivateMines plugin, MineManager mineManager) {
        this.plugin = plugin;
        this.mineManager = mineManager;
    }
    public boolean pregenMines(Player player, int count, String type) {
        Map<String, Map<Material, Double>> mineTypes = mineManager.getMineTypes();
        if (count <= 0 || count > 50) {
            player.sendMessage(ColorUtil.deserialize("&cLe nombre de mines à pré-générer doit être compris entre 1 et 50."));
            return false;
        }
        if (type != null && !mineTypes.containsKey(type)) {
            player.sendMessage(ColorUtil.deserialize("&cType de mine inconnu: &e" + type));
            return false;
        }
        String defaultType = type != null ? type : plugin.getConfigManager().getConfig().getString("Config.Mines.default.type", "default");
        player.sendMessage(ColorUtil.deserialize("&6Pré-génération de &e" + count + " &6mines de type &e" + defaultType + "&6..."));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final AtomicInteger successes = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);
            for (int i = 0; i < count; i++) {
                UUID randomUUID = UUID.randomUUID();
                Location location = plugin.getMineWorldManager().getNextMineLocation();
                if (location == null) {
                    plugin.getLogger().warning("Plus de positions disponibles pour la pré-génération.");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ColorUtil.deserialize("&cPlus de positions disponibles pour la pré-génération."));
                        player.sendMessage(ColorUtil.deserialize("&6" + successes.get() + " mines pré-générées avec succès, " + failures.get() + " échecs."));
                    });
                    return;
                }
                Mine mine = new Mine(randomUUID, location, defaultType);
                mine.setBlocks(mineTypes.get(defaultType));
                final int currentIndex = i + 1;
                AtomicBoolean success = new AtomicBoolean(false);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    success.set(plugin.getMineManager().getMineGenerationService().generateMine(mine));
                    if (success.get()) {
                        plugin.getMineManager().addMineToMap(randomUUID, mine);
                        successes.incrementAndGet();
                        if (plugin.getHologramManager() != null) {
                            plugin.getHologramManager().createOrUpdateHologram(mine);
                        }
                    } else {
                        failures.incrementAndGet();
                    }
                    if (currentIndex == count || currentIndex % 1 == 0) {
                        int percent = (int) (((double) currentIndex / (double) count) * 100);
                        int barLength = 30;
                        int filled = (int) (barLength * (percent / 100.0));
                        StringBuilder bar = new StringBuilder();
                        for (int j = 0; j < barLength; j++) {
                            bar.append(j < filled ? "§a█" : "§7█");
                        }
                        String actionBar = "§ePré-génération : §a" + currentIndex + "/" + count + " §8[" + bar + "§8] §a" + percent + "%";
                        player.sendActionBar(actionBar);
                        if (currentIndex % 5 == 0 || currentIndex == count) {
                            player.sendMessage(ColorUtil.deserialize("&6Progression: &e" + currentIndex + "/" + count + " &6(&a" + successes.get() + " succès&6, &c" + failures.get() + " échecs&6)"));
                        }
                    }
                    if (currentIndex == count) {
                        plugin.getMineManager().saveAllMineData();
                        player.sendMessage(ColorUtil.deserialize("&aPré-génération terminée. Toutes les mines ont été sauvegardées."));
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        return true;
    }
} 