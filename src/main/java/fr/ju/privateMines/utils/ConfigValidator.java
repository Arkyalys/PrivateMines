package fr.ju.privateMines.utils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
public class ConfigValidator {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final List<String> errors;
    public ConfigValidator(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
        this.errors = new ArrayList<>();
    }
    public boolean validateConfig() {
        errors.clear();
        checkSection("Config");
        checkSection("Config.Mines");
        checkSection("Config.Mines.default");
        checkSection("Config.Mines.tiers");
        checkValue("Config.Mines.default.size", "int");
        checkValue("Config.Mines.default.type", "string");
        checkValue("Config.Mines.default.tier", "int");
        if (config.contains("Config.Mines.default.types")) {
            for (String type : config.getConfigurationSection("Config.Mines.default.types").getKeys(false)) {
                checkSection("Config.Mines.default.types." + type + ".blocks");
            }
        }
        if (config.contains("Config.Mines.tiers")) {
            for (String tier : config.getConfigurationSection("Config.Mines.tiers").getKeys(false)) {
                checkSection("Config.Mines.tiers." + tier + ".blocks");
            }
        }
        return errors.isEmpty();
    }
    private void checkSection(String path) {
        if (!config.contains(path)) {
            errors.add("Section manquante: " + path);
        }
    }
    private void checkValue(String path, String type) {
        if (!config.contains(path)) {
            errors.add("Valeur manquante: " + path);
            return;
        }
        switch (type.toLowerCase()) {
            case "int":
                try {
                    config.getInt(path);
                } catch (Exception e) {
                    errors.add("Valeur invalide pour " + path + ": doit être un nombre entier");
                }
                break;
            case "string":
                try {
                    config.getString(path);
                } catch (Exception e) {
                    errors.add("Valeur invalide pour " + path + ": doit être une chaîne de caractères");
                }
                break;
            case "double":
                try {
                    config.getDouble(path);
                } catch (Exception e) {
                    errors.add("Valeur invalide pour " + path + ": doit être un nombre décimal");
                }
                break;
        }
    }
    public List<String> getErrors() {
        return errors;
    }
    public void logErrors() {
        if (!errors.isEmpty()) {
            plugin.getLogger().severe("Erreurs de configuration trouvées:");
            for (String error : errors) {
                plugin.getLogger().severe("- " + error);
            }
        }
    }
} 