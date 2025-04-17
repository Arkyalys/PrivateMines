package fr.ju.privateMines.services;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import fr.ju.privateMines.models.Mine;
public class MineMemoryService {
    private final Map<UUID, Mine> playerMines = new ConcurrentHashMap<>();
    public Map<UUID, Mine> getPlayerMines() {
        return Collections.unmodifiableMap(playerMines);
    }
    public Collection<Mine> getAllMines() {
        return new ArrayList<>(playerMines.values());
    }
    public boolean hasMine(Player player) {
        return playerMines.containsKey(player.getUniqueId());
    }
    public boolean hasMine(UUID uuid) {
        return playerMines.containsKey(uuid);
    }
    public Mine getMine(Player player) {
        return playerMines.get(player.getUniqueId());
    }
    public Mine getMine(UUID uuid) {
        return playerMines.get(uuid);
    }
    public void addMineToMap(UUID uuid, Mine mine) {
        playerMines.put(uuid, mine);
    }
    public void removeMine(UUID uuid) {
        playerMines.remove(uuid);
    }
    public Mine findAvailablePregenMine() {
        for (Map.Entry<UUID, Mine> entry : playerMines.entrySet()) {
            Mine mine = entry.getValue();
            return mine;
        }
        return null;
    }
    public boolean assignPregenMineToPlayer(Mine pregenMine, Player player) {
        if (pregenMine == null || player == null) return false;
        playerMines.remove(pregenMine.getOwner());
        try {
            java.lang.reflect.Field ownerField = Mine.class.getDeclaredField("owner");
            ownerField.setAccessible(true);
            ownerField.set(pregenMine, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
        playerMines.put(player.getUniqueId(), pregenMine);
        return true;
    }
    public void clearPlayerMines() {
        playerMines.clear();
    }
} 