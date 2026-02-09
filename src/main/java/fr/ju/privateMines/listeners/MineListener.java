package fr.ju.privateMines.listeners;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
public class MineListener implements Listener {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineListener(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineManager = plugin.getMineManager();
        this.configManager = plugin.getConfigManager();
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        logDebugHeader(player);
        if (handleMineInMemory(player)) return;
        configManager.reloadData();
        ConfigurationSection minesSection = configManager.getData().getConfigurationSection("mines");
        logMinesSection(minesSection, player);
        if (minesSection != null && minesSection.contains(player.getUniqueId().toString()) && tryLoadMineFromFile(player, minesSection)) return;
        handleAutoMineAssignment(player);
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (mineManager.hasMine(player)) {
            mineManager.saveMineData(player);
        }
    }
    private void logDebugHeader(Player player) {
        PrivateMines.debugLog("=========== LOGS DE DEBUG MINE ===========");
        PrivateMines.debugLog("Joueur connecté: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
        PrivateMines.debugLog("Mines en mémoire: " + mineManager.mineMemoryService.getPlayerMines().size());
    }
    private boolean handleMineInMemory(Player player) {
        boolean mineEnMemoire = mineManager.hasMine(player);
        PrivateMines.debugLog("Le joueur a-t-il une mine en mémoire? " + mineEnMemoire);
        if (mineEnMemoire) {
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine == null) return true;
            PrivateMines.debugLog("Détails de la mine: Taille=" + mine.getSize() + ", Tier=" + mine.getTier());
            return true;
        }
        return false;
    }
    private void logMinesSection(ConfigurationSection minesSection, Player player) {
        PrivateMines.debugLog("Section 'mines' dans data.yml existe? " + (minesSection != null));
        if (minesSection != null) {
            PrivateMines.debugLog("data.yml contient-il l'UUID du joueur? " + minesSection.contains(player.getUniqueId().toString()));
        }
    }
    private boolean tryLoadMineFromFile(Player player, ConfigurationSection minesSection) {
        UUID playerUUID = player.getUniqueId();
        PrivateMines.debugLog("Le joueur " + player.getName() + " a une mine dans le fichier de données mais pas en mémoire. Tentative de récupération...");
        ConfigurationSection mineSection = minesSection.getConfigurationSection(playerUUID.toString());
        if (mineSection == null) {
            PrivateMines.debugLog("Section de mine null pour " + player.getName());
            return false;
        }
        try {
            String worldName = mineSection.getString("world");
            PrivateMines.debugLog("Nom du monde récupéré: " + worldName);
            World world = resolveWorld(worldName);
            if (world != null) {
                Mine mineLoaded = buildMineFromSection(playerUUID, mineSection, world);
                mineManager.addMineToMap(playerUUID, mineLoaded);
                PrivateMines.debugLog("Mine récupérée et ajoutée en mémoire pour " + player.getName());
                boolean success = mineManager.assignPregenMineToPlayer(mineLoaded, player);
                if (success) {
                    Mine mine = mineManager.getMine(player).orElse(null);
                    if (mine != null) {
                        player.teleport(mineManager.getBetterTeleportLocation(mine));
                    }
                    player.sendMessage(configManager.getMessage("mine-assigned"));
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du rechargement de la mine pour " + player.getName() + ": " + e.getMessage());
            plugin.getErrorHandler().logError("Erreur lors du rechargement de la mine pour " + player.getName(), e);
        }
        return false;
    }
    private World resolveWorld(String worldName) {
        World world = null;
        if (worldName == null || worldName.isEmpty()) {
            PrivateMines.debugLog("Nom du monde absent ou vide, tentative d'utilisation du monde par défaut");
            if (plugin.getMineWorldManager() != null) {
                world = plugin.getMineWorldManager().getMineWorld();
                PrivateMines.debugLog("Monde par défaut récupéré: " + (world != null ? world.getName() : "null"));
            }
        } else {
            world = plugin.getServer().getWorld(worldName);
            PrivateMines.debugLog("Monde trouvé via son nom: " + (world != null ? "oui" : "non"));
            if (world == null && plugin.getMineWorldManager() != null) {
                world = plugin.getMineWorldManager().getMineWorld();
                PrivateMines.debugLog("Utilisation du monde par défaut car le monde spécifié n'existe pas: " + (world != null ? world.getName() : "null"));
            }
        }
        return world;
    }
    private Mine buildMineFromSection(UUID playerUUID, ConfigurationSection mineSection, World world) {
        double x = mineSection.getDouble("x");
        double y = mineSection.getDouble("y");
        double z = mineSection.getDouble("z");
        PrivateMines.debugLog("Position: " + x + "," + y + "," + z);
        Location location = new Location(world, x, y, z);
        Mine mineLoaded = new Mine(playerUUID, location);
        mineLoaded.setSize(mineSection.getInt("size", 1));
        mineLoaded.setTax(mineSection.getInt("tax", 0));
        mineLoaded.setOpen(mineSection.getBoolean("isOpen", true));
        mineLoaded.setTier(mineSection.getInt("tier", 1));
        PrivateMines.debugLog("Propriétés: Size=" + mineLoaded.getSize() + ", Tax=" + mineLoaded.getTax() + ", Open=" + mineLoaded.isOpen() + ", Tier=" + mineLoaded.getTier());
        if (mineSection.contains("area")) {
            int minX = mineSection.getInt("area.minX");
            int minY = mineSection.getInt("area.minY");
            int minZ = mineSection.getInt("area.minZ");
            int maxX = mineSection.getInt("area.maxX");
            int maxY = mineSection.getInt("area.maxY");
            int maxZ = mineSection.getInt("area.maxZ");
            mineLoaded.setMineArea(minX, minY, minZ, maxX, maxY, maxZ);
            PrivateMines.debugLog("Zone de mine: " + minX + "," + minY + "," + minZ + " à " + maxX + "," + maxY + "," + maxZ);
        }
        if (mineSection.contains("teleport")) {
            String tpWorldName = mineSection.getString("teleport.world", world.getName());
            World tpWorld = plugin.getServer().getWorld(tpWorldName);
            if (tpWorld == null) tpWorld = world;
            double tpX = mineSection.getDouble("teleport.x");
            double tpY = mineSection.getDouble("teleport.y");
            double tpZ = mineSection.getDouble("teleport.z");
            float tpYaw = (float) mineSection.getDouble("teleport.yaw", 0);
            float tpPitch = (float) mineSection.getDouble("teleport.pitch", 0);
            Location teleportLocation = new Location(tpWorld, tpX, tpY, tpZ, tpYaw, tpPitch);
            mineLoaded.setTeleportLocation(teleportLocation);
            PrivateMines.debugLog("Point de téléportation: " + tpX + "," + tpY + "," + tpZ);
        }
        if (mineSection.contains("blocks")) {
            ConfigurationSection blocksSection = mineSection.getConfigurationSection("blocks");
            Map<Material, Double> blocks = new HashMap<>();
            if (blocksSection != null) {
                for (String materialName : blocksSection.getKeys(false)) {
                    try {
                        Material material = Material.valueOf(materialName);
                        double chance = blocksSection.getDouble(materialName);
                        blocks.put(material, chance);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Type de bloc invalide: " + materialName);
                    }
                }
            }
            if (!blocks.isEmpty()) {
                mineLoaded.setBlocks(blocks);
                PrivateMines.debugLog("Blocs définis: " + blocks.size() + " types");
            }
        }
        return mineLoaded;
    }
    private void handleAutoMineAssignment(Player player) {
        boolean giveMineOnJoin = configManager.getConfig().getBoolean("Config.Give-Mine-On-Join", true);
        PrivateMines.debugLog("Give-Mine-On-Join activé? " + giveMineOnJoin);
        if (giveMineOnJoin) {
            boolean pregenEnabled = configManager.getConfig().getBoolean("Config.Pregen-Mine-Assignment.enabled", true);
            boolean preferExisting = configManager.getConfig().getBoolean("Config.Pregen-Mine-Assignment.prefer-existing-mines", true);
            PrivateMines.debugLog("Attribution auto: activée=" + pregenEnabled + ", préférer existantes=" + preferExisting);
            if (pregenEnabled && preferExisting) {
                Mine availableMine = mineManager.findAvailablePregenMine();
                PrivateMines.debugLog("Mine pré-générée disponible? " + (availableMine != null));
                if (availableMine != null) {
                    boolean success = mineManager.assignPregenMineToPlayer(availableMine, player);
                    PrivateMines.debugLog("Attribution réussie? " + success);
                    if (success) {
                        player.teleport(mineManager.getBetterTeleportLocation(availableMine));
                        player.sendMessage(configManager.getMessage("mine-assigned"));
                        return;
                    }
                }
            }
            PrivateMines.debugLog("Création d'une nouvelle mine pour " + player.getName());
            mineManager.createMine(player);
        }
    }
}
