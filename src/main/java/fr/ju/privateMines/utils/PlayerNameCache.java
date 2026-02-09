package fr.ju.privateMines.utils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import fr.ju.privateMines.PrivateMines;

/**
 * Cache thread-safe pour les noms de joueurs.
 * Évite les appels coûteux à Bukkit.getOfflinePlayer() qui font du I/O disque.
 */
public class PlayerNameCache implements Listener {

    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    private final ConcurrentHashMap<UUID, CachedName> cache = new ConcurrentHashMap<>();

    private static class CachedName {
        final String name;
        final long timestamp;

        CachedName(String name) {
            this.name = name;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Récupère le nom d'un joueur depuis le cache, ou fait le lookup si nécessaire.
     * Thread-safe.
     */
    public String getName(UUID uuid) {
        if (uuid == null) return "???";

        // Check en ligne d'abord (gratuit)
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            cache.put(uuid, new CachedName(online.getName()));
            return online.getName();
        }

        // Check cache
        CachedName cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.name;
        }

        // Fallback: lookup coûteux (une seule fois par 30 min)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        if (name != null) {
            cache.put(uuid, new CachedName(name));
            return name;
        }

        return uuid.toString().substring(0, 8);
    }

    /**
     * Pré-remplit le cache quand un joueur se connecte.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cache.put(player.getUniqueId(), new CachedName(player.getName()));
    }

    /**
     * Nettoie les entrées expirées du cache.
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public int size() {
        return cache.size();
    }
}
