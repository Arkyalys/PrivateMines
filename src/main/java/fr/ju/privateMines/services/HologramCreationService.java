package fr.ju.privateMines.services;
import java.util.List;
import org.bukkit.Location;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.HologramManager;
import fr.ju.privateMines.models.Mine;
public class HologramCreationService {
    private final PrivateMines plugin;
    private final HologramManager hologramManager;
    public HologramCreationService(PrivateMines plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }
    public void createOrUpdateSingleHologram(Mine mine, String holoName, Location holoLocation, List<String> holoLines) {
        Hologram existingHologram = DHAPI.getHologram(holoName);
        if (existingHologram != null) {
            try {
                DHAPI.setHologramLines(existingHologram, holoLines);
                DHAPI.moveHologram(existingHologram, holoLocation);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la mise à jour de l'hologramme " + holoName + ". Suppression et recréation...");
                try {
                    DHAPI.removeHologram(holoName);
                    DHAPI.createHologram(holoName, holoLocation, holoLines);
                } catch (Exception ex) {
                    plugin.getLogger().severe("Erreur lors de la recréation de l'hologramme " + holoName + ": " + ex.getMessage());
                }
            }
        } else {
            try {
                DHAPI.createHologram(holoName, holoLocation, holoLines);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("already exists")) {
                    plugin.getLogger().warning("Un hologramme existant a été détecté avec le même nom. Suppression et recréation...");
                    try {
                        DHAPI.removeHologram(holoName);
                        DHAPI.createHologram(holoName, holoLocation, holoLines);
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Erreur lors de la recréation de l'hologramme: " + ex.getMessage());
                    }
                } else {
                    plugin.getLogger().severe("Erreur lors de la création de l'hologramme: " + e.getMessage());
                }
            }
        }
    }
} 