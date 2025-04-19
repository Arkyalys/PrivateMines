package fr.ju.privateMines.services;
import java.util.Map;
import java.util.UUID;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.HologramManager;
public class HologramDeleteService {
    private final HologramManager hologramManager;
    public HologramDeleteService(PrivateMines plugin, HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }
    public void removeHologram(UUID ownerId) {
        Map<String, String> holoMap = hologramManager.mineHolograms.get(ownerId);
        if (holoMap != null) {
            for (String holoName : holoMap.values()) {
                Hologram hologram = DHAPI.getHologram(holoName);
                if (hologram != null) {
                    DHAPI.removeHologram(holoName);
                }
            }
            hologramManager.mineHolograms.remove(ownerId);
        }
    }
} 