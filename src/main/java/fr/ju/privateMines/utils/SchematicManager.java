package fr.ju.privateMines.utils;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import com.sk89q.worldedit.math.BlockVector3;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class SchematicManager {
    private final PrivateMines plugin;
    private FAWESchematicManager faweManager;
    public SchematicManager(PrivateMines plugin) {
        this.plugin = plugin;
        try {
            this.faweManager = new FAWESchematicManager(plugin);
            plugin.getLogger().info("FastAsyncWorldEdit initialisé avec succès pour les opérations de schématiques");
        } catch (Throwable e) {
            plugin.getLogger().severe("Erreur critique lors de l'initialisation de FAWE: " + e.getMessage());
            plugin.getLogger().severe("FAWE est une dépendance obligatoire. Vérifiez votre installation.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }
    public boolean isUsingFAWE() {
        return true; 
    }
    public BlockVector3[] pasteSchematic(String schematicName, Location location) {
        final BlockVector3[][] result = new BlockVector3[1][1];
        faweManager.pasteSchematicAsync(schematicName, location, bounds -> {
            result[0] = bounds;
        });
        long timeout = System.currentTimeMillis() + 10000; 
        while (result[0] == null && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result[0];
    }
    public void pasteSchematicAsync(String schematicName, Location location, java.util.function.Consumer<BlockVector3[]> callback) {
        faweManager.pasteSchematicAsync(schematicName, location, callback);
    }
    public boolean deleteMineStructure(Mine mine) {
        faweManager.deleteMineStructureAsync(mine, success -> {
            plugin.getLogger().info("Mine d'ID " + mine.getOwner() + " supprimée de façon asynchrone: " + success);
        });
        return true;
    }
    public void deleteMineStructureAsync(Mine mine, java.util.function.Consumer<Boolean> callback) {
        faweManager.deleteMineStructureAsync(mine, callback);
    }
    public BlockVector3 getSchematicDimensions(String schematicName) {
        return faweManager.getSchematicDimensions(schematicName);
    }
    public void clearCache() {
        faweManager.clearCache();
    }
} 