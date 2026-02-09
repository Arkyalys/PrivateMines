package fr.ju.privateMines.utils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.bukkit.plugin.java.JavaPlugin;
public class ErrorHandler {
    private final JavaPlugin plugin;
    private final File errorLogFile;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public ErrorHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.errorLogFile = new File(plugin.getDataFolder(), "errors.log");
        if (!errorLogFile.exists()) {
            try {
                errorLogFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier de log d'erreurs (" + errorLogFile.getAbsolutePath() + "): " + e.getMessage());
            }
        }
    }
    public void logError(String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] %s", timestamp, message);
        plugin.getLogger().severe(logMessage);
        if (throwable != null) {
            throwable.printStackTrace();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(errorLogFile, true))) {
            writer.println(logMessage);
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
            writer.println(); 
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'écrire dans le fichier de log: " + e.getMessage());
        }
    }
    public void logWarning(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] WARNING: %s", timestamp, message);
        plugin.getLogger().warning(message);
        try (PrintWriter writer = new PrintWriter(new FileWriter(errorLogFile, true))) {
            writer.println(logMessage);
            writer.println();
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'écrire dans le fichier de log: " + e.getMessage());
        }
    }
    public void logInfo(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] INFO: %s", timestamp, message);
        plugin.getLogger().info(message);
        try (PrintWriter writer = new PrintWriter(new FileWriter(errorLogFile, true))) {
            writer.println(logMessage);
            writer.println();
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'écrire dans le fichier de log: " + e.getMessage());
        }
    }
} 