package fr.ju.privateMines.utils;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
public class ParticleManager {
    private final PrivateMines plugin;
    private final Random random = new Random();
    private final Map<UUID, ParticleEffect> playerEffects = new HashMap<>();
    private BukkitTask particleTask;
    public ParticleManager(PrivateMines plugin) {
        this.plugin = plugin;
        startParticleTask();
    }
    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, ParticleEffect> entry : playerEffects.entrySet()) {
                UUID playerId = entry.getKey();
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null || !player.isOnline()) continue;
                Mine mine = plugin.getMineManager().getMine(playerId).orElse(null);
                if (mine == null) continue;
                displayEffect(player, mine, entry.getValue());
            }
        }, 5L, 5L); 
    }
    private void displayEffect(Player player, Mine mine, ParticleEffect effect) {
        if (mine.getLocation() == null || mine.getLocation().getWorld() == null) return;
        Location center = mine.getLocation();
        World world = center.getWorld();
        switch (effect) {
            case UPGRADE_CELEBRATION:
                displayUpgradeCelebration(world, center);
                break;
            case RESET_SWIRL:
                displayResetSwirl(world, center);
                break;
            case ACTIVE_MINE:
                displayActiveMine(world, mine);
                break;
            case RAINBOW_BORDER:
                displayRainbowBorder(world, mine);
                break;
        }
    }
    private void displayUpgradeCelebration(World world, Location center) {
        for (int i = 0; i < 3; i++) {
            double offsetX = random.nextDouble() * 10 - 5; 
            double offsetZ = random.nextDouble() * 10 - 5; 
            Location loc = center.clone().add(offsetX, 5, offsetZ);
            Color color = Color.fromRGB(
                    random.nextInt(255), 
                    random.nextInt(255), 
                    random.nextInt(255));
            DustOptions dustOptions = new DustOptions(color, 1.0F);
            for (double y = 0; y < 5; y += 0.25) {
                Location particleLoc = loc.clone().add(0, y, 0);
                world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
            }
            Location explosionLoc = loc.clone().add(0, 5, 0);
            world.spawnParticle(Particle.FLAME, explosionLoc, 5, 0.5, 0.5, 0.5, 0.05);
            world.spawnParticle(Particle.HEART, explosionLoc, 30, 1.5, 1.5, 1.5, 0.1);
        }
    }
    private void displayResetSwirl(World world, Location center) {
        double radius = 8.0;
        double particlesPerCircle = 20;
        double maxCircles = 10;
        for (double height = 0; height < maxCircles; height += 0.5) {
            double angle = (System.currentTimeMillis() / 200.0) + (height * Math.PI / 3);
            double y = height - maxCircles / 2;
            for (int i = 0; i < particlesPerCircle; i++) {
                double circleAngle = 2 * Math.PI * i / particlesPerCircle + angle;
                double x = radius * Math.cos(circleAngle);
                double z = radius * Math.sin(circleAngle);
                Location loc = center.clone().add(x, y + 5, z);
                world.spawnParticle(Particle.PORTAL, loc, 1, 0, 0, 0, 0);
            }
        }
    }
    private void displayActiveMine(World world, Mine mine) {
        if (!mine.hasMineArea()) return;
        int minX = mine.getMinX();
        int maxX = mine.getMaxX();
        int maxY = mine.getMaxY();
        int minZ = mine.getMinZ();
        int maxZ = mine.getMaxZ();
        for (int i = 0; i < 5; i++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);
            Location loc = new Location(world, x, maxY + 0.5, z);
            world.spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0.1, 0.2, 0.01);
        }
    }
    private void displayRainbowBorder(World world, Mine mine) {
        if (!mine.hasMineArea()) return;
        int minX = mine.getMinX();
        int maxX = mine.getMaxX();
        int minZ = mine.getMinZ();
        int maxZ = mine.getMaxZ();
        int y = mine.getMinY();
        double time = System.currentTimeMillis() / 1000.0;
        for (int x = minX; x <= maxX; x++) {
            displayRainbowParticle(world, x, y, minZ, time + x * 0.1);
            displayRainbowParticle(world, x, y, maxZ, time + x * 0.1);
        }
        for (int z = minZ; z <= maxZ; z++) {
            displayRainbowParticle(world, minX, y, z, time + z * 0.1);
            displayRainbowParticle(world, maxX, y, z, time + z * 0.1);
        }
    }
    private void displayRainbowParticle(World world, int x, int y, int z, double offset) {
        double frequency = 0.3;
        int red = (int) (Math.sin(frequency * offset + 0) * 127 + 128);
        int green = (int) (Math.sin(frequency * offset + 2) * 127 + 128);
        int blue = (int) (Math.sin(frequency * offset + 4) * 127 + 128);
        Color color = Color.fromRGB(red, green, blue);
        DustOptions dustOptions = new DustOptions(color, 1.0F);
        Location loc = new Location(world, x + 0.5, y + 0.1, z + 0.5);
        world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dustOptions);
    }
    public void activateEffect(Player player, ParticleEffect effect, int duration) {
        UUID uuid = player.getUniqueId();
        playerEffects.put(uuid, effect);
        plugin.getServer().getScheduler().runTaskLater(plugin, 
                () -> playerEffects.remove(uuid), duration);
    }
    public void playUpgradeEffect(Player player) {
        activateEffect(player, ParticleEffect.UPGRADE_CELEBRATION, 100); 
    }
    public void playResetEffect(Player player) {
        activateEffect(player, ParticleEffect.RESET_SWIRL, 60); 
    }
    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        playerEffects.clear();
    }
    public enum ParticleEffect {
        UPGRADE_CELEBRATION, 
        RESET_SWIRL,         
        ACTIVE_MINE,         
        RAINBOW_BORDER       
    }
} 