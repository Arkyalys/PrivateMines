package fr.ju.privateMines.utils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.java.JavaPlugin;
public class ErrorHandler {
    private final JavaPlugin plugin;
    private final File errorLogFile;
    private final ExecutorService writeExecutor;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public ErrorHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.errorLogFile = new File(plugin.getDataFolder(), "errors.log");
        this.writeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PrivateMines-ErrorLog");
            t.setDaemon(true);
            return t;
        });
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
        String stackTrace = null;
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        }
        final String finalStackTrace = stackTrace;
        writeExecutor.submit(() -> writeToFile(logMessage, finalStackTrace));
    }
    public void logWarning(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] WARNING: %s", timestamp, message);
        plugin.getLogger().warning(message);
        writeExecutor.submit(() -> writeToFile(logMessage, null));
    }
    public void logInfo(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logMessage = String.format("[%s] INFO: %s", timestamp, message);
        plugin.getLogger().info(message);
        writeExecutor.submit(() -> writeToFile(logMessage, null));
    }
    private void writeToFile(String logMessage, String stackTrace) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(errorLogFile, true))) {
            writer.println(logMessage);
            if (stackTrace != null) {
                writer.print(stackTrace);
            }
            writer.println();
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible d'écrire dans le fichier de log: " + e.getMessage());
        }
    }
    public void shutdown() {
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
