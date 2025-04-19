package fr.ju.privateMines.managers;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class MineTeleportService {
    public MineTeleportService(PrivateMines plugin) {
    }
    public boolean teleportToMine(Player owner, Player visitor, Mine mine) {
        return false; 
    }
    public Location getBetterTeleportLocation(Mine mine) {
        return null; 
    }
} 