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
        if (count <= 0 || count > 50) {
            player.sendMessage(ColorUtil.deserialize("&cLe nombre de mines à pré-générer doit être compris entre 1 et 50."));
            return false;
        }
        player.sendMessage(ColorUtil.deserialize("&6Pré-génération de &e" + count + " &6mines..."));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> runPregenTask(player, count));
        return true;
    }

    private void runPregenTask(Player player, int count) {
        // Logique de pré-génération sans notion de type
        // ...
    }

    private void runPregenTask(Player player, int count, String defaultType, Map<String, Map<Material, Double>> mineTypes) {
        final AtomicInteger successes = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);
        for (int i = 0; i < count; i++) {
            UUID randomUUID = UUID.randomUUID();
            Location location = plugin.getMineWorldManager().getNextMineLocation();
            if (location == null) {
                handleNoLocationAvailable(player, successes, failures);
                return;
            }
            Mine mine = new Mine(randomUUID, location);
            // Par défaut, tout en pierre
            Map<Material, Double> defaultBlocks = new java.util.HashMap<>();
            defaultBlocks.put(Material.STONE, 1.0);
            mine.setBlocks(defaultBlocks);
            final int currentIndex = i + 1;
            AtomicBoolean success = new AtomicBoolean(false);
            plugin.getServer().getScheduler().runTask(plugin, () -> handleMineGeneration(player, mine, randomUUID, successes, failures, currentIndex, count));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleNoLocationAvailable(Player player, AtomicInteger successes, AtomicInteger failures) {
        plugin.getLogger().warning("Plus de positions disponibles pour la pré-génération.");
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage(ColorUtil.deserialize("&cPlus de positions disponibles pour la pré-génération."));
            player.sendMessage(ColorUtil.deserialize("&6" + successes.get() + " mines pré-générées avec succès, " + failures.get() + " échecs."));
        });
    }

    private void handleMineGeneration(Player player, Mine mine, UUID randomUUID, AtomicInteger successes, AtomicInteger failures, int currentIndex, int count) {
        boolean generated = plugin.getMineManager().getMineGenerationService().generateMine(mine);
        if (generated) {
            plugin.getMineManager().addMineToMap(randomUUID, mine);
            successes.incrementAndGet();
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().createOrUpdateHologram(mine);
            }
        } else {
            failures.incrementAndGet();
        }
        updateProgressBar(player, currentIndex, count, successes, failures);
        if (currentIndex == count) {
            plugin.getMineManager().saveAllMineData();
            player.sendMessage(ColorUtil.deserialize("&aPré-génération terminée. Toutes les mines ont été sauvegardées."));
        }
    }

    private void updateProgressBar(Player player, int currentIndex, int count, AtomicInteger successes, AtomicInteger failures) {
        int percent = (int) (((double) currentIndex / (double) count) * 100);
        int barLength = 30;
        int filled = (int) (barLength * (percent / 100.0));
        StringBuilder bar = new StringBuilder();
        for (int j = 0; j < barLength; j++) {
            bar.append(j < filled ? "§a█" : "§7█");
        }
        String actionBar = "§ePré-génération : §a" + currentIndex + "/" + count + " §8[" + bar + "§8] §a" + percent + "%";
        player.sendActionBar(net.kyori.adventure.text.Component.text(actionBar));
        if (currentIndex % 5 == 0 || currentIndex == count) {
            player.sendMessage(ColorUtil.deserialize("&6Progression: &e" + currentIndex + "/" + count + " &6(&a" + successes.get() + " succès&6, &c" + failures.get() + " échecs&6)"));
        }
    }
} 