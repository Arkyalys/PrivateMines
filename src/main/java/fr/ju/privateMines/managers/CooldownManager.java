package fr.ju.privateMines.managers;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.ConfigManager;
public class CooldownManager {
    private final ConfigManager configManager;
    private final Map<UUID, Long> resetCooldowns;
    public CooldownManager(PrivateMines plugin) {
        this.configManager = plugin.getConfigManager();
        this.resetCooldowns = new HashMap<>();
    }
    public boolean isOnCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!resetCooldowns.containsKey(uuid)) {
            return false;
        }
        long cooldownTime = configManager.getConfig().getInt("Config.Cooldown-Reset-Command") * 60 * 1000L;
        long lastReset = resetCooldowns.get(uuid);
        long currentTime = System.currentTimeMillis();
        return currentTime - lastReset < cooldownTime;
    }
    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (!resetCooldowns.containsKey(uuid)) {
            return 0;
        }
        long cooldownTime = configManager.getConfig().getInt("Config.Cooldown-Reset-Command") * 60 * 1000L;
        long lastReset = resetCooldowns.get(uuid);
        long currentTime = System.currentTimeMillis();
        return Math.max(0, (lastReset + cooldownTime - currentTime) / 1000);
    }
    public void setCooldown(Player player) {
        resetCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    public void removeCooldown(Player player) {
        resetCooldowns.remove(player.getUniqueId());
    }
} 