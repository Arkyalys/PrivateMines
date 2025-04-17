package fr.ju.privateMines.utils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
public class DependencyManager {
    private final Map<String, Boolean> dependencies;
    private final Map<String, String> dependencyVersions;
    public DependencyManager() {
        this.dependencies = new HashMap<>();
        this.dependencyVersions = new HashMap<>();
    }
    public void checkDependencies() {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null) {
            dependencies.put("WorldGuard", true);
            dependencyVersions.put("WorldGuard", worldGuard.getPluginMeta().getVersion());
        } else {
            dependencies.put("WorldGuard", false);
        }
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit != null) {
            dependencies.put("WorldEdit", true);
            dependencyVersions.put("WorldEdit", worldEdit.getPluginMeta().getVersion());
        } else {
            dependencies.put("WorldEdit", false);
        }
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null) {
            dependencies.put("PlaceholderAPI", true);
            dependencyVersions.put("PlaceholderAPI", placeholderAPI.getPluginMeta().getVersion());
        } else {
            dependencies.put("PlaceholderAPI", false);
        }
    }
    public boolean isDependencyPresent(String dependency) {
        return dependencies.getOrDefault(dependency, false);
    }
    public String getDependencyVersion(String dependency) {
        return dependencyVersions.getOrDefault(dependency, "Non installé");
    }
    public boolean areRequiredDependenciesPresent() {
        return isDependencyPresent("WorldGuard") && isDependencyPresent("WorldEdit");
    }
} 