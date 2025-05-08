package fr.ju.privateMines.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;

/**
 * Classe utilitaire pour valider les données du plugin et gérer la corruption
 */
public class DataValidator {
    private final PrivateMines plugin;
    
    public DataValidator(PrivateMines plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Valide un fichier de configuration YAML
     * @param file Le fichier à valider
     * @return true si le fichier est valide, false sinon
     */
    public boolean validateYamlFile(File file) {
        if (!file.exists()) {
            plugin.getLogger().warning("Le fichier " + file.getName() + " n'existe pas");
            return false;
        }
        
        try {
            YamlConfiguration.loadConfiguration(file);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur de validation du fichier " + file.getName() + ": " + e.getMessage());
            createBackup(file);
            return false;
        }
    }
    
    /**
     * Crée une sauvegarde d'un fichier corrompu
     * @param file Le fichier à sauvegarder
     */
    public void createBackup(File file) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File(file.getParentFile(), file.getName() + "." + timestamp + ".bak");
            
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Sauvegarde créée: " + backupFile.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de créer une sauvegarde de " + file.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Vérifie les données d'une mine et effectue les corrections si nécessaire
     * @param mine La mine à valider
     * @return true si la mine est valide ou a été corrigée, false si elle est invalide et n'a pas pu être corrigée
     */
    public boolean validateMine(Mine mine) {
        if (mine == null) {
            return false;
        }
        
        boolean valid = true;
        
        // Validation de l'UUID
        if (mine.getOwner() == null) {
            plugin.getLogger().severe("Mine avec UUID propriétaire null détectée");
            return false;
        }
        
        // Validation de l'emplacement
        if (mine.getLocation() == null) {
            plugin.getLogger().warning("Mine sans emplacement détectée (UUID: " + mine.getOwner() + ")");
            valid = false;
        } else if (mine.getLocation().getWorld() == null && plugin.getMineWorldManager() != null) {
            plugin.getLogger().warning("Mine avec monde null détectée (UUID: " + mine.getOwner() + "). Tentative de correction...");
            Location fixedLocation = mine.getLocation().clone();
            fixedLocation.setWorld(plugin.getMineWorldManager().getMineWorld());
            mine.setLocation(fixedLocation);
            plugin.getLogger().info("Emplacement de la mine corrigé pour UUID: " + mine.getOwner());
        }
        
        // Validation des blocs
        if (mine.getBlocks() == null || mine.getBlocks().isEmpty()) {
            plugin.getLogger().warning("Mine sans blocs détectée (UUID: " + mine.getOwner() + "). Assignation des blocs par défaut...");
            if (plugin.getMineManager().getMineTiers().containsKey(mine.getTier())) {
                mine.setBlocks(plugin.getMineManager().getMineTiers().get(mine.getTier()));
                plugin.getLogger().info("Blocs par défaut assignés pour UUID: " + mine.getOwner());
            } else {
                plugin.getLogger().warning("Impossible d'assigner des blocs par défaut (tier invalide): " + mine.getTier());
                valid = false;
            }
        }
        
        // Validation des dimensions de la mine
        if (!mine.hasMineArea()) {
            plugin.getLogger().warning("Mine sans dimensions détectée (UUID: " + mine.getOwner() + ")");
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Vérifie une section de configuration de mine et tente la récupération
     * @param section La section de configuration
     * @param key La clé (UUID) de la mine
     * @return true si la section est valide ou a été corrigée, false sinon
     */
    public boolean validateMineSection(ConfigurationSection section, String key) {
        if (section == null) {
            plugin.getLogger().severe("Section de configuration null pour UUID: " + key);
            return false;
        }
        
        boolean valid = true;
        
        // Validation de l'UUID
        try {
            UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("UUID invalide dans les données: " + key);
            return false;
        }
        
        // Validation de l'emplacement
        if (!section.contains("location") || !section.isConfigurationSection("location")) {
            plugin.getLogger().severe("Section 'location' manquante ou invalide pour UUID: " + key);
            valid = false;
        } else {
            ConfigurationSection locationSection = section.getConfigurationSection("location");
            if (!locationSection.contains("world") || !locationSection.contains("x") || 
                !locationSection.contains("y") || !locationSection.contains("z")) {
                plugin.getLogger().severe("Coordonnées manquantes dans 'location' pour UUID: " + key);
                valid = false;
            }
        }
        
        // Validation de la zone de mine
        if (!section.contains("area") || !section.isConfigurationSection("area")) {
            plugin.getLogger().warning("Section 'area' manquante ou invalide pour UUID: " + key);
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Restaure un fichier de données à partir d'une sauvegarde
     * @param dataFile Le fichier de données à restaurer
     * @return true si la restauration a réussi
     */
    public boolean restoreFromBackup(File dataFile) {
        File dataDir = dataFile.getParentFile();
        File[] backups = dataDir.listFiles((dir, name) -> name.startsWith(dataFile.getName()) && name.endsWith(".bak"));
        
        if (backups == null || backups.length == 0) {
            plugin.getLogger().severe("Aucune sauvegarde trouvée pour " + dataFile.getName());
            return false;
        }
        
        // Trouver la sauvegarde la plus récente
        File latestBackup = backups[0];
        for (File backup : backups) {
            if (backup.lastModified() > latestBackup.lastModified()) {
                latestBackup = backup;
            }
        }
        
        try {
            // Créer une sauvegarde du fichier corrompu
            createBackup(dataFile);
            
            // Restaurer depuis la sauvegarde
            Files.copy(latestBackup.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Fichier " + dataFile.getName() + " restauré depuis " + latestBackup.getName());
            
            return validateYamlFile(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Échec de la restauration depuis la sauvegarde: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Essaie de corriger un objet mine avec des valeurs par défaut
     * @param mine La mine à corriger
     */
    public void applyDefaultValues(Mine mine) {
        if (mine == null) return;
        
        // Appliquer les valeurs par défaut pour les blocs
        if (mine.getBlocks() == null) {
            mine.setBlocks(new HashMap<>());
        }
        
        if (mine.getBlocks().isEmpty()) {
            mine.getBlocks().put(Material.STONE, 100.0);
        }
        
        // S'assurer que le tier est valide
        if (mine.getTier() <= 0) {
            mine.setTier(1);
        }
        
        // S'assurer que la taille est valide
        if (mine.getSize() <= 0) {
            mine.setSize(10);
        }
        
        // Vérifier que l'accès est initialisé
        mine.getMineAccess();
    }
    
    /**
     * Vérifie et corrige le fichier de données principal
     * @return true si le fichier a été validé ou corrigé avec succès
     */
    public boolean validateAndRepairDataFile() {
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        
        if (!validateYamlFile(dataFile)) {
            plugin.getLogger().severe("Fichier data.yml corrompu! Tentative de restauration...");
            if (!restoreFromBackup(dataFile)) {
                // Si la restauration échoue, on crée un fichier vide
                try {
                    if (!dataFile.exists()) {
                        dataFile.createNewFile();
                    }
                    FileConfiguration emptyConfig = YamlConfiguration.loadConfiguration(dataFile);
                    emptyConfig.createSection("mines");
                    emptyConfig.save(dataFile);
                    plugin.getLogger().warning("Un fichier data.yml vide a été créé. Les mines précédentes sont perdues.");
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().severe("Impossible de créer un nouveau fichier data.yml: " + e.getMessage());
                    return false;
                }
            }
        }
        
        return true;
    }
} 