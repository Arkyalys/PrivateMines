package fr.ju.privateMines.managers;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Material;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.services.MineAreaService;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.MineAreaDetector;
import fr.ju.privateMines.utils.SchematicManager;
public class MineGenerationService {
    private final PrivateMines plugin;
    private final ConfigManager configManager;
    private final MineProtectionManager protectionManager;
    private final SchematicManager schematicManager;
    private final MineAreaService mineAreaService;
    public MineGenerationService(PrivateMines plugin, MineProtectionManager protectionManager, MineAreaDetector areaDetector, SchematicManager schematicManager) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.protectionManager = protectionManager;
        this.schematicManager = schematicManager;
        this.mineAreaService = new MineAreaService(plugin, areaDetector, protectionManager);
    }
    public void generateMineAsync(Mine mine, Consumer<Boolean> callback) {
        generateMineAsync(mine, callback, null);
    }
    public void generateMineAsync(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player) {
        String schematicName = configManager.getConfig().getString("Mines.default.schematic", "mine.schem");
        if (schematicName == null || schematicName.isEmpty()) {
            plugin.getLogger().warning("Aucun schematic défini pour la mine (clé Mines.default.schematic)");
            callback.accept(false);
            return;
        }
        
        // Définir les étapes de génération de la mine
        generateMineInSteps(mine, callback, player, schematicName);
    }
    private void generateMineInSteps(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player, String schematicName) {
        // Étape 1 : Collage du schéma
        updateProgress(player, 1, 4, "Collage du schéma...");
        pasteSchematicAndContinue(mine, callback, player, schematicName);
    }
    private void pasteSchematicAndContinue(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player, String schematicName) {
        schematicManager.pasteSchematicAsync(schematicName, mine.getLocation(), bounds -> {
            if (!validateBounds(bounds, mine, callback)) {
                return;
            }
            
            // Étape 2 : Protection
            updateProgress(player, 2, 4, "Protection de la mine...");
            protectMineAndContinue(mine, callback, player, bounds);
        });
    }
    private boolean validateBounds(com.sk89q.worldedit.math.BlockVector3[] bounds, Mine mine, Consumer<Boolean> callback) {
        if (bounds == null || bounds.length < 2 || bounds[0] == null || bounds[1] == null) {
            plugin.getLogger().warning("Erreur lors du collage du schematic pour la mine: bounds invalides");
            callback.accept(false);
            return false;
        }
        
        mine.setSchematicBounds(
            bounds[0].getX(), bounds[0].getY(), bounds[0].getZ(),
            bounds[1].getX(), bounds[1].getY(), bounds[1].getZ()
        );
        
        return true;
    }
    private void protectMineAndContinue(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player, com.sk89q.worldedit.math.BlockVector3[] bounds) {
        protectionManager.protectMine(mine, bounds);
        
        // Étape 3 : Détection de la zone
        updateProgress(player, 3, 4, "Détection de la zone...");
        setupMineAreaAndContinue(mine, callback, player);
    }
    private void setupMineAreaAndContinue(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player) {
        boolean areaOk = mineAreaService.setupMineArea(mine);
        if (!areaOk) {
            plugin.getLogger().warning("Zone de minage non détectée après le collage du schematic pour la mine: " + mine.getOwner());
        }
        
        // Étape 4 : Hologramme
        updateProgress(player, 4, 4, "Création de l'hologramme...");
        createHologramAndFinish(mine, callback, player);
    }
    private void createHologramAndFinish(Mine mine, Consumer<Boolean> callback, org.bukkit.entity.Player player) {
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().createOrUpdateHologram(mine);
        }
        
        // Fin
        if (player != null) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§aMine créée avec succès !"));
        }
        callback.accept(true);
    }
    private void updateProgress(org.bukkit.entity.Player player, int step, int total, String label) {
        if (player == null) return;
        
        int percent = (int) (((double) step / (double) total) * 100);
        int barLength = 30;
        int filled = (int) (barLength * (percent / 100.0));
        
        StringBuilder bar = new StringBuilder();
        for (int j = 0; j < barLength; j++) {
            bar.append(j < filled ? "§a█" : "§7█");
        }
        
        String actionBar = "§eCréation de la mine : §8[" + bar + "§8] §a" + percent + "% §7- " + label;
        player.sendActionBar(net.kyori.adventure.text.Component.text(actionBar));
    }
    public boolean generateMine(Mine mine) {
        final boolean[] result = new boolean[1];
        final boolean[] completed = new boolean[1];
        generateMineAsync(mine, success -> {
            result[0] = success;
            completed[0] = true;
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return completed[0] && result[0];
    }
    public void createDefaultMineArea(Mine mine) {
    }
    public void fillMineWithOres(Mine mine, Map<Integer, Map<Material, Double>> mineTiers) {
    }
    public Material getRandomBlock(Map<Material, Double> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            plugin.getLogger().severe("[Reset Debug] getRandomBlock a reçu une map de blocs vide ou nulle! Retourne STONE.");
            return Material.STONE;
        }
        double total = blocks.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            plugin.getLogger().warning("[Reset Debug] getRandomBlock: somme des probabilités nulle ou négative (" + total + ") dans la map: " + blocks.toString() + ". Retourne STONE.");
            return Material.STONE;
        }
        double random = Math.random() * total;
        double current = 0;
        for (Map.Entry<Material, Double> entry : blocks.entrySet()) {
            current += entry.getValue();
            if (random <= current) {
                return entry.getKey();
            }
        }
        plugin.getLogger().warning("[Reset Debug] getRandomBlock: n'a pas pu sélectionner de bloc malgré un total > 0 (" + total + ") et la map: " + blocks.toString() + ". Retourne STONE.");
        return Material.STONE;
    }
    public String createProgressBar(int percentage) {
        String completedColor = plugin.getConfigManager().getConfig().getString("Config.Gameplay.progress-bars.completed-color", "&a");
        String remainingColor = plugin.getConfigManager().getConfig().getString("Config.Gameplay.progress-bars.remaining-color", "&7");
        String borderColor = plugin.getConfigManager().getConfig().getString("Config.Gameplay.progress-bars.border-color", "&8");
        String character = plugin.getConfigManager().getConfig().getString("Config.Gameplay.progress-bars.character", "■");
        int barLength = plugin.getConfigManager().getConfig().getInt("Config.Gameplay.progress-bars.length", 20);
        StringBuilder bar = new StringBuilder(borderColor + "[");
        int completedLength = (int) Math.round(percentage / (100.0 / barLength));
        bar.append(completedColor);
        for (int i = 0; i < completedLength; i++) {
            bar.append(character);
        }
        bar.append(remainingColor);
        for (int i = 0; i < barLength - completedLength; i++) {
            bar.append(character);
        }
        bar.append(borderColor + "]");
        return bar.toString();
    }
} 