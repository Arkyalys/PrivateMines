package fr.ju.privateMines.utils;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.ju.privateMines.PrivateMines;
public class ConfigManager {
    private final PrivateMines plugin;
    private FileConfiguration config;
    private File configFile;
    private FileConfiguration messages;
    private File messagesFile;
    private FileConfiguration data;
    private File dataFile;
    private FileConfiguration tiers;
    private File tiersFile;

    // Valeurs config cachées (évite les lookups YAML répétés sur les hot paths)
    private volatile int autoResetThreshold;
    private volatile int maxMineSize;
    private volatile int maxTax;
    private volatile int expandCost;
    private volatile boolean autoResetEnabled;

    public ConfigManager(PrivateMines plugin) {
        this.plugin = plugin;
        setupConfig();
        setupMessages();
        setupData();
        setupTiers();
        refreshCachedValues();
    }

    private void refreshCachedValues() {
        this.autoResetThreshold = config.getInt("Config.Gameplay.auto-reset.threshold", 65);
        this.maxMineSize = config.getInt("Config.Mines.max-size", 100);
        this.maxTax = config.getInt("Config.Gameplay.max-tax", 100);
        this.expandCost = config.getInt("Config.Gameplay.expand-cost", 100);
        this.autoResetEnabled = config.getBoolean("Config.Gameplay.auto-reset.enabled", true);
    }

    public int getAutoResetThreshold() { return autoResetThreshold; }
    public int getMaxMineSize() { return maxMineSize; }
    public int getMaxTax() { return maxTax; }
    public int getExpandCost() { return expandCost; }
    public boolean isAutoResetEnabled() { return autoResetEnabled; }
    private void setupConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    private void setupMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    private void setupData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getErrorHandler().logError("Erreur lors de la création du fichier data.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }
    private void setupTiers() {
        tiersFile = new File(plugin.getDataFolder(), "tiers.yml");
        if (!tiersFile.exists()) {
            plugin.saveResource("tiers.yml", false);
        }
        tiers = YamlConfiguration.loadConfiguration(tiersFile);
    }
    public FileConfiguration getConfig() {
        return config;
    }
    public FileConfiguration getMessages() {
        return messages;
    }
    public FileConfiguration getTiersConfig() {
        return tiers;
    }
    public String getMessage(String path) {
        String message = messages.getString(path);
        if (message == null) return "";
        return ColorUtil.translateColors(message);
    }
    public String getMessageOrDefault(String path, String defaultValue) {
        String message = messages.getString(path);
        if (message == null) {
            messages.set(path, defaultValue);
            saveMessages();
            message = defaultValue;
        }
        return ColorUtil.translateColors(message);
    }
    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }
        return message;
    }
    public FileConfiguration getData() {
        return data;
    }
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getErrorHandler().logError("Erreur lors de la sauvegarde du fichier config.yml", e);
        }
    }
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getErrorHandler().logError("Erreur lors de la sauvegarde du fichier messages.yml", e);
        }
    }
    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getErrorHandler().logError("Erreur lors de la sauvegarde du fichier data.yml", e);
        }
    }
    public void saveTiers() {
        try {
            tiers.save(tiersFile);
        } catch (IOException e) {
            plugin.getErrorHandler().logError("Erreur lors de la sauvegarde du fichier tiers.yml", e);
        }
    }
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        tiers = YamlConfiguration.loadConfiguration(tiersFile);
        refreshCachedValues();
    }
    public void reloadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }
} 