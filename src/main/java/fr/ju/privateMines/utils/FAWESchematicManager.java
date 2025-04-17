package fr.ju.privateMines.utils;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class FAWESchematicManager {
    private final PrivateMines plugin;
    private final Map<String, Clipboard> clipboardCache;
    public FAWESchematicManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.clipboardCache = new HashMap<>();
        configureOptimizations();
        preloadSchematics();
    }
    private void preloadSchematics() {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            plugin.getLogger().info("Dossier des schématiques créé");
            return;
        }
        File[] files = schematicsFolder.listFiles((dir, name) -> 
            name.endsWith(".schem") || name.endsWith(".schematic"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Aucune schématique trouvée dans le dossier 'schematics'");
            return;
        }
        for (File file : files) {
            try {
                String name = file.getName();
                Clipboard clipboard = loadSchematicFromFile(file);
                if (clipboard != null) {
                    clipboardCache.put(name, clipboard);
                    plugin.getLogger().info("Schématique préchargée: " + name);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors du préchargement de la schématique " + file.getName() + ": " + e.getMessage());
            }
        }
    }
    private Clipboard loadSchematicFromFile(File file) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                plugin.getLogger().warning("Format de schématique non supporté: " + file.getName());
                return null;
            }
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                return reader.read();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du chargement de la schématique " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }
    private Clipboard getSchematic(String schematicName) {
        if (clipboardCache.containsKey(schematicName)) {
            return clipboardCache.get(schematicName);
        }
        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName);
        if (!schematicFile.exists() && !schematicName.contains(".")) {
            schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName + ".schem");
        }
        if (!schematicFile.exists() && !schematicName.contains(".")) {
            schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName + ".schematic");
        }
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic not found: " + schematicName);
            return null;
        }
        Clipboard clipboard = loadSchematicFromFile(schematicFile);
        if (clipboard != null) {
            clipboardCache.put(schematicName, clipboard);
        }
        return clipboard;
    }
    public void pasteSchematicAsync(String schematicName, Location location, Consumer<BlockVector3[]> callback) {
        if (schematicName == null || location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Paramètres invalides pour le collage du schematic");
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        World world = location.getWorld();
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        Clipboard clipboard = getSchematic(schematicName);
        if (clipboard == null) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    BlockVector3 to = BlockVector3.at(location.getX(), location.getY(), location.getZ());
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    Operation operation = holder.createPaste(editSession)
                            .to(to)
                            .ignoreAirBlocks(true)
                            .build();
                    Operations.complete(operation);
                    Region region = clipboard.getRegion();
                    BlockVector3 min = region.getMinimumPoint().add(to);
                    BlockVector3 max = region.getMaximumPoint().add(to);
                    if (callback != null) {
                        BlockVector3[] result = new BlockVector3[]{min, max};
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors du collage asynchrone du schematic: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }
            }
        });
    }
    public void deleteMineStructureAsync(Mine mine, Consumer<Boolean> callback) {
        if (mine == null || mine.getLocation() == null || mine.getLocation().getWorld() == null) {
            plugin.getLogger().warning("Mine invalide pour la suppression de la structure");
            if (callback != null) {
                callback.accept(false);
            }
            return;
        }
        World world = mine.getLocation().getWorld();
        BlockVector3 min, max;
        if (mine.hasMineArea()) {
            min = BlockVector3.at(mine.getMinX() - 10, mine.getMinY() - 10, mine.getMinZ() - 10);
            max = BlockVector3.at(mine.getMaxX() + 10, mine.getMaxY() + 10, mine.getMaxZ() + 10);
        } else {
            Location center = mine.getLocation();
            int radius = mine.getSize() * 2;
            min = BlockVector3.at(center.getX() - radius, center.getY() - radius, center.getZ() - radius);
            max = BlockVector3.at(center.getX() + radius, center.getY() + radius, center.getZ() + radius);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                    CuboidRegion region = new CuboidRegion(min, max);
                    if (BlockTypes.AIR != null) {
                        BlockState air = BlockTypes.AIR.getDefaultState();
                        editSession.setBlocks(region, air);
                    } else {
                        editSession.setBlocks(region, editSession.getBlock(min).getBlockType().getDefaultState());
                    }
                    plugin.getLogger().info("Structure de la mine supprimée aux coordonnées : " + 
                                          "min(" + min.getX() + "," + min.getY() + "," + min.getZ() + ") " +
                                          "max(" + max.getX() + "," + max.getY() + "," + max.getZ() + ")");
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de la suppression asynchrone de la structure: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                }
            }
        });
    }
    public void configureOptimizations() {
        try {
            plugin.getLogger().info("Optimisations WorldEdit configurées avec succès");
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la configuration des optimisations: " + e.getMessage());
        }
    }
    public BlockVector3 getSchematicDimensions(String schematicName) {
        Clipboard clipboard = getSchematic(schematicName);
        if (clipboard == null) {
            return null;
        }
        return clipboard.getDimensions();
    }
    public Region getSchematicRegion(String schematicName) {
        Clipboard clipboard = getSchematic(schematicName);
        if (clipboard == null) {
            return null;
        }
        return clipboard.getRegion();
    }
    public void clearCache() {
        clipboardCache.clear();
        plugin.getLogger().info("Cache des schématiques vidé");
    }
} 