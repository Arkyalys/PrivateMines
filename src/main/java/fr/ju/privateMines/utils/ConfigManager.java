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
    public ConfigManager(PrivateMines plugin) {
        this.plugin = plugin;
        setupConfig();
        setupMessages();
        setupData();
    }
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
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }
    public FileConfiguration getConfig() {
        return config;
    }
    public FileConfiguration getMessages() {
        return messages;
    }
    public String getMessage(String path) {
        String message = messages.getString(path);
        if (message == null) return "";
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
            e.printStackTrace();
        }
    }
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    public void reloadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }
} 