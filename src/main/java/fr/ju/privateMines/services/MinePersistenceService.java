package fr.ju.privateMines.services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.managers.MineProtectionManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineAccess;
import fr.ju.privateMines.utils.ConfigManager;
public class MinePersistenceService {
    private final PrivateMines plugin;
    public MinePersistenceService(PrivateMines plugin) {
        this.plugin = plugin;
    }
    public void saveMineData(Player player, MineManager mineManager) {
        plugin.getLogger().info("[DEBUG-MINE] Sauvegarde des données pour " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
        if (!mineManager.hasMine(player)) {
            plugin.getLogger().warning("[DEBUG-MINE] Tentative de sauvegarder les données pour un joueur sans mine: " + player.getName());
            return;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            plugin.getLogger().severe("[DEBUG-MINE] Mine null alors que hasMine retourne true pour: " + player.getName());
            return;
        }
        saveMine(mine, mineManager);
        plugin.getLogger().info("[DEBUG-MINE] Sauvegarde terminée pour " + player.getName());
        try {
            plugin.getConfigManager().saveData();
            plugin.getLogger().info("[DEBUG-MINE] Fichier data.yml sauvegardé avec succès");
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG-MINE] Erreur lors de la sauvegarde du fichier data.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void saveMine(Mine mine, MineManager mineManager) {
        String uuid = mine.getOwner().toString();
        plugin.getLogger().info("[DEBUG-MINE] Sauvegarde de la mine pour UUID: " + uuid);
        ConfigManager configManager = plugin.getConfigManager();
        ConfigurationSection mineSection = configManager.getData().createSection("mines." + uuid);
        if (mine.getLocation() == null) {
            plugin.getLogger().severe("[DEBUG-MINE] Location est null pour la mine de UUID: " + uuid);
            return;
        }
        if (mine.getLocation().getWorld() == null) {
            plugin.getLogger().severe("[DEBUG-MINE] Monde est null pour la mine de UUID: " + uuid);
            if (plugin.getMineWorldManager() != null && plugin.getMineWorldManager().getMineWorld() != null) {
                Location newLoc = mine.getLocation().clone();
                newLoc.setWorld(plugin.getMineWorldManager().getMineWorld());
                mine.setLocation(newLoc);
                plugin.getLogger().info("[DEBUG-MINE] Monde défini sur le monde par défaut: " + newLoc.getWorld().getName());
            } else {
                plugin.getLogger().severe("[DEBUG-MINE] Impossible de définir un monde par défaut, la sauvegarde peut être incomplète");
            }
        }
        if (mine.getLocation().getWorld() != null) {
            mineSection.set("location.world", mine.getLocation().getWorld().getName());
        } else {
            plugin.getLogger().severe("[DEBUG-MINE] Impossible de sauvegarder le nom du monde (est toujours null)");
        }
        mineSection.set("location.x", mine.getLocation().getX());
        mineSection.set("location.y", mine.getLocation().getY());
        mineSection.set("location.z", mine.getLocation().getZ());
        mineSection.set("world", null);
        mineSection.set("type", mine.getType());
        mineSection.set("size", mine.getSize());
        mineSection.set("tax", mine.getTax());
        mineSection.set("isOpen", mine.isOpen());
        mineSection.set("tier", mine.getTier());
        if (mine.getTeleportLocation() != null) {
            if (mine.getTeleportLocation().getWorld() != null) {
                mineSection.set("teleport.world", mine.getTeleportLocation().getWorld().getName());
            } else if (mine.getLocation().getWorld() != null) {
                mineSection.set("teleport.world", mine.getLocation().getWorld().getName());
                plugin.getLogger().warning("[DEBUG-MINE] Le monde du point de téléportation est null, utilisation du monde principal de la mine");
            }
            mineSection.set("teleport.x", mine.getTeleportLocation().getX());
            mineSection.set("teleport.y", mine.getTeleportLocation().getY());
            mineSection.set("teleport.z", mine.getTeleportLocation().getZ());
            mineSection.set("teleport.yaw", mine.getTeleportLocation().getYaw());
            mineSection.set("teleport.pitch", mine.getTeleportLocation().getPitch());
        }
        if (mine.hasMineArea()) {
            mineSection.set("area.minX", mine.getMinX());
            mineSection.set("area.minY", mine.getMinY());
            mineSection.set("area.minZ", mine.getMinZ());
            mineSection.set("area.maxX", mine.getMaxX());
            mineSection.set("area.maxY", mine.getMaxY());
            mineSection.set("area.maxZ", mine.getMaxZ());
        }
        if (mine.hasSchematicBounds()) {
            mineSection.set("schematic.minX", mine.getSchematicMinX());
            mineSection.set("schematic.minY", mine.getSchematicMinY());
            mineSection.set("schematic.minZ", mine.getSchematicMinZ());
            mineSection.set("schematic.maxX", mine.getSchematicMaxX());
            mineSection.set("schematic.maxY", mine.getSchematicMaxY());
            mineSection.set("schematic.maxZ", mine.getSchematicMaxZ());
        }
        ConfigurationSection blocksSection = mineSection.createSection("blocks");
        for (Map.Entry<Material, Double> entry : mine.getBlocks().entrySet()) {
            blocksSection.set(entry.getKey().name(), entry.getValue());
        }
        saveAccessData(mine, mineSection);
        plugin.getConfigManager().saveData();
    }
    public void saveAccessData(Mine mine, ConfigurationSection mineSection) {
        MineAccess access = mine.getMineAccess();
        java.util.List<String> permanentBans = new java.util.ArrayList<>();
        for (UUID bannedUser : access.getPermanentBannedUsers()) {
            permanentBans.add(bannedUser.toString());
        }
        mineSection.set("access.permanent-bans", permanentBans);
        ConfigurationSection tempBansSection = mineSection.createSection("access.temp-bans");
        for (Map.Entry<UUID, Long> entry : access.getTemporaryBannedUsers().entrySet()) {
            tempBansSection.set(entry.getKey().toString(), entry.getValue());
        }
        java.util.List<String> deniedUsers = new java.util.ArrayList<>();
        for (UUID deniedUser : access.getDeniedUsers()) {
            deniedUsers.add(deniedUser.toString());
        }
        mineSection.set("access.denied-users", deniedUsers);
    }
    public void loadAccessData(Mine mine, ConfigurationSection mineSection) {
        if (mineSection == null || !mineSection.contains("access")) {
            return;
        }
        MineAccess access = mine.getMineAccess();
        java.util.List<String> permanentBans = mineSection.getStringList("access.permanent-bans");
        for (String uuidString : permanentBans) {
            try {
                UUID bannedUser = UUID.fromString(uuidString);
                access.addPermanentBan(bannedUser);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in permanent bans: " + uuidString);
            }
        }
        if (mineSection.contains("access.temp-bans")) {
            ConfigurationSection tempBansSection = mineSection.getConfigurationSection("access.temp-bans");
            for (String uuidString : tempBansSection.getKeys(false)) {
                try {
                    UUID bannedUser = UUID.fromString(uuidString);
                    long expiration = tempBansSection.getLong(uuidString);
                    if (expiration > System.currentTimeMillis()) {
                        long remainingTime = (expiration - System.currentTimeMillis()) / 1000;
                        access.addTemporaryBan(bannedUser, remainingTime);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in temporary bans: " + uuidString);
                }
            }
        }
        java.util.List<String> deniedUsers = mineSection.getStringList("access.denied-users");
        for (String uuidString : deniedUsers) {
            try {
                UUID deniedUser = UUID.fromString(uuidString);
                access.addDeniedUser(deniedUser);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in denied users: " + uuidString);
            }
        }
    }
    public void loadMineData(MineManager mineManager, ConfigManager configManager, PrivateMines plugin, MineProtectionManager protectionManager, Map<String, Map<Material, Double>> mineTypes) {
        plugin.getLogger().info("Chargement des données des mines...");
        configManager.reloadData();
        mineManager.mineMemoryService.clearPlayerMines();
        ConfigurationSection minesSection = configManager.getData().getConfigurationSection("mines");
        if (minesSection == null) {
            plugin.getLogger().info("Aucune donnée de mine trouvée.");
            return;
        }
        int count = 0;
        int errorCount = 0;
        plugin.getLogger().info("UUIDs trouvés dans le fichier de données: " + String.join(", ", minesSection.getKeys(false)));
        for (String key : minesSection.getKeys(false)) {
            try {
                plugin.getLogger().info("Tentative de chargement de la mine pour UUID: " + key);
                ConfigurationSection mineSection = minesSection.getConfigurationSection(key);
                if (mineSection == null) {
                    plugin.getLogger().warning("Section de configuration nulle pour UUID: " + key);
                    continue;
                }
                UUID ownerUUID;
                try {
                    ownerUUID = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().severe("UUID invalide: " + key + " - " + e.getMessage());
                    errorCount++;
                    continue;
                }
                String worldName = mineSection.getString("location.world");
                if (worldName == null) {
                    plugin.getLogger().severe("Nom du monde manquant pour UUID: " + key);
                    errorCount++;
                    continue;
                }
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Le monde '" + worldName + "' n'existe pas pour la mine de " + key + ". Création du monde...");
                    if (plugin.getMineWorldManager() != null) {
                        world = plugin.getMineWorldManager().getMineWorld();
                        if (world == null) {
                            plugin.getLogger().severe("Impossible de créer le monde. La mine de " + key + " ne sera pas chargée.");
                            errorCount++;
                            continue;
                        }
                    } else {
                        plugin.getLogger().severe("MineWorldManager non disponible. La mine de " + key + " ne sera pas chargée.");
                        errorCount++;
                        continue;
                    }
                }
                double x = mineSection.getDouble("location.x");
                double y = mineSection.getDouble("location.y");
                double z = mineSection.getDouble("location.z");
                Location location = new Location(world, x, y, z);
                plugin.getLogger().info("Position de la mine pour UUID " + key + ": " + world.getName() + ", " + x + ", " + y + ", " + z);
                String type = mineSection.getString("type", "default");
                Mine mine = new Mine(ownerUUID, location, type);
                mine.setSize(mineSection.getInt("size", 1));
                mine.setTax(mineSection.getInt("tax", 0));
                mine.setOpen(mineSection.getBoolean("isOpen", true));
                mine.setTier(mineSection.getInt("tier", 1));
                if (mineSection.contains("teleport")) {
                    String tpWorldName = mineSection.getString("teleport.world", worldName);
                    World tpWorld = plugin.getServer().getWorld(tpWorldName);
                    if (tpWorld == null) tpWorld = world;
                    double tpX = mineSection.getDouble("teleport.x");
                    double tpY = mineSection.getDouble("teleport.y");
                    double tpZ = mineSection.getDouble("teleport.z");
                    float tpYaw = (float) mineSection.getDouble("teleport.yaw", 0);
                    float tpPitch = (float) mineSection.getDouble("teleport.pitch", 0);
                    Location teleportLocation = new Location(tpWorld, tpX, tpY, tpZ, tpYaw, tpPitch);
                    mine.setTeleportLocation(teleportLocation);
                }
                if (mineSection.contains("area")) {
                    int minX = mineSection.getInt("area.minX");
                    int minY = mineSection.getInt("area.minY");
                    int minZ = mineSection.getInt("area.minZ");
                    int maxX = mineSection.getInt("area.maxX");
                    int maxY = mineSection.getInt("area.maxY");
                    int maxZ = mineSection.getInt("area.maxZ");
                    mine.setMineArea(minX, minY, minZ, maxX, maxY, maxZ);
                    plugin.getLogger().info("Zone de la mine définie: " + minX + "," + minY + "," + minZ + " à " + maxX + "," + maxY + "," + maxZ);
                }
                if (mineSection.contains("schematic")) {
                    double minX = mineSection.getDouble("schematic.minX");
                    double minY = mineSection.getDouble("schematic.minY");
                    double minZ = mineSection.getDouble("schematic.minZ");
                    double maxX = mineSection.getDouble("schematic.maxX");
                    double maxY = mineSection.getDouble("schematic.maxY");
                    double maxZ = mineSection.getDouble("schematic.maxZ");
                    mine.setSchematicBounds(minX, minY, minZ, maxX, maxY, maxZ);
                    plugin.getLogger().info("[DEBUG-LOAD] Bounds schematic pour la mine " + key + " : min=(" + minX + ", " + minY + ", " + minZ + "), max=(" + maxX + ", " + maxY + ", " + maxZ + ")");
                }
                if (mineSection.contains("blocks")) {
                    ConfigurationSection blocksSection = mineSection.getConfigurationSection("blocks");
                    Map<Material, Double> blocks = new HashMap<>();
                    for (String materialName : blocksSection.getKeys(false)) {
                        try {
                            Material material = Material.valueOf(materialName);
                            double chance = blocksSection.getDouble(materialName);
                            blocks.put(material, chance);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Type de bloc invalide: " + materialName);
                        }
                    }
                    mine.setBlocks(blocks);
                } else {
                    if (mineTypes.containsKey(type)) {
                        mine.setBlocks(mineTypes.get(type));
                    }
                }
                loadAccessData(mine, mineSection);
                mineManager.addMineToMap(ownerUUID, mine);
                count++;
                plugin.getLogger().info("Mine chargée avec succès pour UUID: " + key);
                plugin.getLogger().info("UUID " + ownerUUID + " ajouté à la map. Taille actuelle: " + mineManager.mineMemoryService.getPlayerMines().size());
                if (protectionManager != null) {
                    protectionManager.protectMine(mine, null);
                }
                if (plugin.getStatsManager() != null) {
                    plugin.getStatsManager().syncMineStats(mine);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors du chargement de la mine " + key + ": " + e.getMessage());
                e.printStackTrace();
                errorCount++;
            }
        }
        plugin.getLogger().info(count + " mines ont été chargées avec succès. " + errorCount + " mines ont échoué au chargement.");
        StringBuilder uuids = new StringBuilder("UUIDs des mines en mémoire: ");
        for (UUID uuid : mineManager.mineMemoryService.getPlayerMines().keySet()) {
            uuids.append(uuid.toString()).append(", ");
        }
        plugin.getLogger().info(uuids.toString());
    }
    public void saveAllMineData(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        plugin.getLogger().info("Sauvegarde des données de toutes les mines (" + mineManager.mineMemoryService.getPlayerMines().size() + " mines)...");
        if (plugin.getConfigManager().getData().contains("mines")) {
            plugin.getConfigManager().getData().set("mines", null);
        }
        int savedCount = 0;
        for (Map.Entry<UUID, Mine> entry : mineManager.mineMemoryService.getPlayerMines().entrySet()) {
            UUID ownerId = entry.getKey();
            Mine mine = entry.getValue();
            if (mine == null || mine.getLocation() == null || mine.getLocation().getWorld() == null) {
                plugin.getLogger().warning("Mine invalide détectée pour l'UUID " + ownerId + ". Ignorée lors de la sauvegarde.");
                continue;
            }
            String path = "mines." + ownerId.toString();
            plugin.getConfigManager().getData().set(path + ".location.world", mine.getLocation().getWorld().getName());
            plugin.getConfigManager().getData().set(path + ".location.x", mine.getLocation().getX());
            plugin.getConfigManager().getData().set(path + ".location.y", mine.getLocation().getY());
            plugin.getConfigManager().getData().set(path + ".location.z", mine.getLocation().getZ());
            plugin.getConfigManager().getData().set(path + ".type", mine.getType());
            plugin.getConfigManager().getData().set(path + ".tier", mine.getTier());
            plugin.getConfigManager().getData().set(path + ".size", mine.getSize());
            plugin.getConfigManager().getData().set(path + ".isOpen", mine.isOpen());
            plugin.getConfigManager().getData().set(path + ".tax", mine.getTax());
            if (mine.hasMineArea()) {
                plugin.getConfigManager().getData().set(path + ".area.minX", mine.getMinX());
                plugin.getConfigManager().getData().set(path + ".area.minY", mine.getMinY());
                plugin.getConfigManager().getData().set(path + ".area.minZ", mine.getMinZ());
                plugin.getConfigManager().getData().set(path + ".area.maxX", mine.getMaxX());
                plugin.getConfigManager().getData().set(path + ".area.maxY", mine.getMaxY());
                plugin.getConfigManager().getData().set(path + ".area.maxZ", mine.getMaxZ());
            }
            if (mine.hasSchematicBounds()) {
                plugin.getConfigManager().getData().set(path + ".schematic.minX", mine.getSchematicMinX());
                plugin.getConfigManager().getData().set(path + ".schematic.minY", mine.getSchematicMinY());
                plugin.getConfigManager().getData().set(path + ".schematic.minZ", mine.getSchematicMinZ());
                plugin.getConfigManager().getData().set(path + ".schematic.maxX", mine.getSchematicMaxX());
                plugin.getConfigManager().getData().set(path + ".schematic.maxY", mine.getSchematicMaxY());
                plugin.getConfigManager().getData().set(path + ".schematic.maxZ", mine.getSchematicMaxZ());
            }
            if (mine.getTeleportLocation() != null) {
                plugin.getConfigManager().getData().set(path + ".teleport.world", mine.getTeleportLocation().getWorld().getName());
                plugin.getConfigManager().getData().set(path + ".teleport.x", mine.getTeleportLocation().getX());
                plugin.getConfigManager().getData().set(path + ".teleport.y", mine.getTeleportLocation().getY());
                plugin.getConfigManager().getData().set(path + ".teleport.z", mine.getTeleportLocation().getZ());
                plugin.getConfigManager().getData().set(path + ".teleport.yaw", mine.getTeleportLocation().getYaw());
                plugin.getConfigManager().getData().set(path + ".teleport.pitch", mine.getTeleportLocation().getPitch());
            }
            if (mine.getBlocks() != null && !mine.getBlocks().isEmpty()) {
                for (Map.Entry<Material, Double> blockEntry : mine.getBlocks().entrySet()) {
                    plugin.getConfigManager().getData().set(path + ".blocks." + blockEntry.getKey().name(), blockEntry.getValue());
                }
            }
            saveAccessData(mine, plugin.getConfigManager().getData().createSection(path + ".access"));
            savedCount++;
        }
        plugin.getConfigManager().saveData();
        plugin.getLogger().info("Sauvegarde terminée. " + savedCount + " mines enregistrées.");
    }
} 