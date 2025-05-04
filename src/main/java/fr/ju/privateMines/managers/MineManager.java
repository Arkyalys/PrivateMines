package fr.ju.privateMines.managers;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.services.MineMemoryService;
import fr.ju.privateMines.services.MinePersistenceService;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.MineAreaDetector;
import fr.ju.privateMines.utils.SchematicManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
public class MineManager {
    private final PrivateMines plugin;
    private final ConfigManager configManager;
    private final MineProtectionManager protectionManager;
    private final Map<Integer, Map<Material, Double>> mineTiers;
    private final MineGenerationService mineGenerationService;
    private final MineResetService mineResetService;
    private final MineTaxService mineTaxService;
    private final MineDeleteService mineDeleteService;
    private final MineUpgradeService mineUpgradeService;
    private final MinePregenService minePregenService;
    private final MinePersistenceService minePersistenceService;
    public final MineMemoryService mineMemoryService;
    public MineManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.protectionManager = new MineProtectionManager(plugin);
        this.mineTiers = new HashMap<>();
        MineAreaDetector areaDetector = new MineAreaDetector(plugin);
        SchematicManager schematicManager = new SchematicManager(plugin);
        this.mineGenerationService = new MineGenerationService(plugin, protectionManager, areaDetector, schematicManager);
        this.mineResetService = new MineResetService(plugin);
        this.mineTaxService = new MineTaxService(plugin, this);
        this.mineDeleteService = new MineDeleteService(plugin, this);
        this.mineUpgradeService = new MineUpgradeService(plugin, this);
        this.minePregenService = new MinePregenService(plugin, this);
        this.minePersistenceService = new MinePersistenceService(plugin);
        this.mineMemoryService = new MineMemoryService();
        loadMineTiers();
        loadMineData();
    }
    public void loadMineTiers() {
        mineTiers.clear();
        plugin.getLogger().info("Chargement des tiers de mines...");
        
        org.bukkit.configuration.file.FileConfiguration tiersConfig = plugin.getConfigManager().getTiersConfig();
        if (tiersConfig == null) {
            plugin.getLogger().warning("Erreur: Impossible de charger le fichier tiers.yml");
            return;
        }
        
        if (!tiersConfig.isConfigurationSection("tiers")) {
            plugin.getLogger().warning("Erreur: La section 'tiers' n'existe pas dans tiers.yml");
            return;
        }
        
        org.bukkit.configuration.ConfigurationSection tiersSection = tiersConfig.getConfigurationSection("tiers");
        if (tiersSection == null) {
            plugin.getLogger().warning("Erreur: La section 'tiers' est invalide dans tiers.yml");
            return;
        }
        
        for (String tierKey : tiersSection.getKeys(false)) {
            try {
                int tier = Integer.parseInt(tierKey);
                Map<Material, Double> blocks = new HashMap<>();
                
                org.bukkit.configuration.ConfigurationSection blocksSection = tiersSection.getConfigurationSection(tierKey + ".blocks");
                if (blocksSection != null) {
                    for (String blockKey : blocksSection.getKeys(false)) {
                        try {
                            Material material = Material.valueOf(blockKey);
                            double chance = blocksSection.getDouble(blockKey);
                            blocks.put(material, chance);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Erreur: Matériau invalide '" + blockKey + "' dans le tier " + tier);
                        }
                    }
                    
                    mineTiers.put(tier, blocks);
                    if (PrivateMines.isDebugMode()) {
                        plugin.getLogger().info("Tier " + tier + " chargé avec " + blocks.size() + " blocs");
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Erreur: Tier invalide '" + tierKey + "' dans tiers.yml");
            }
        }
        
        plugin.getLogger().info(mineTiers.size() + " tiers de mines chargés");
    }
    public Collection<Mine> getAllMines() {
        return mineMemoryService.getAllMines();
    }
    /**
     * Log un message de debug si le mode debug est activé.
     */
    private void debug(String message) {
        if (PrivateMines.isDebugMode()) plugin.getLogger().info(message);
    }
    /**
     * Récupère un message de configuration avec remplacements.
     */
    private String getMessage(String key) {
        return configManager.getMessage(key);
    }
    /**
     * Vérifie si un joueur possède une mine.
     */
    public boolean hasMine(Player player) {
        boolean result = mineMemoryService.hasMine(player);
        debug("[DEBUG-MINE] hasMine(" + player.getName() + ") = " + result + ", UUID = " + player.getUniqueId());
        return result;
    }
    /**
     * Vérifie si un UUID possède une mine.
     */
    public boolean hasMine(UUID uuid) {
        boolean result = mineMemoryService.hasMine(uuid);
        debug("[DEBUG-MINE] hasMine(UUID:" + uuid + ") = " + result);
        return result;
    }
    /**
     * Récupère la mine d'un joueur sous forme d'Optional.
     */
    public Optional<Mine> getMine(Player player) {
        Mine mine = mineMemoryService.getMine(player);
        debug("[DEBUG-MINE] getMine(" + player.getName() + ") = " + (mine != null ? "trouvé" : "null"));
        return Optional.ofNullable(mine);
    }
    /**
     * Récupère la mine d'un UUID sous forme d'Optional.
     */
    public Optional<Mine> getMine(UUID uuid) {
        return Optional.ofNullable(mineMemoryService.getMine(uuid));
    }
    /**
     * Crée une mine pour un joueur. Retourne true si succès, false sinon.
     */
    public boolean createMine(Player player) {
        if (!isValidMineCreation(player)) return false;
        Location location = plugin.getMineWorldManager().getNextMineLocation();
        if (!isValidLocation(player, location)) return false;
        if (hasMine(player)) {
            player.sendMessage(getMessage("Messages.already-own-mine"));
            return false;
        }
        Mine mine = new Mine(player.getUniqueId(), location);
        Map<Material, Double> defaultBlocks = new HashMap<>();
        defaultBlocks.put(Material.STONE, 1.0);
        mine.setBlocks(defaultBlocks);
        player.sendActionBar(net.kyori.adventure.text.Component.text("§eCréation de la mine en cours..."));
        mineGenerationService.generateMineAsync(mine, success -> handleMineGenerationResult(success, player, mine), player);
        return true;
    }
    private boolean isValidMineCreation(Player player) {
        if (player == null) {
            plugin.getLogger().warning("Tentative de création de mine avec un joueur null");
            return false;
        }
        return true;
    }
    private boolean isValidLocation(Player player, Location location) {
        if (location == null) {
            player.sendMessage(getMessage("Messages.no-available-location"));
            return false;
        }
        return true;
    }
    private void handleMineGenerationResult(boolean success, Player player, Mine mine) {
        if (success) {
            debug("[DEBUG] Mine generation successful for " + player.getName() + ". Adding to map...");
            mineMemoryService.addMineToMap(player.getUniqueId(), mine);
            debug("[DEBUG] Mine added to map for UUID: " + player.getUniqueId() + ". Map size: " + mineMemoryService.getPlayerMines().size());
            if (mine.hasMineArea()) {
                mine.calculateTotalBlocks();
                mine.getStats().resetBlockStats();
                mine.synchronizeStats();
                debug("[DEBUG] New mine statistics initialized: totalBlocks=" + mine.getStats().getTotalBlocks() + ", blocksMined=" + mine.getStats().getBlocksMined());
            }
            saveMineData(player);
            debug("[DEBUG] Called saveMineData for " + player.getName());
            player.sendActionBar(net.kyori.adventure.text.Component.text("§aMine créée avec succès !"));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            player.sendMessage(getMessage("Messages.mine-created"));
            player.sendMessage(getMessage("Messages.teleported-to-mine-after-creation"));
            teleportPlayerToMine(player, mine);
            Title title = createMineCreatedTitle(player);
            player.showTitle(title);
        } else {
            plugin.getLogger().warning("Échec de la génération de la mine pour " + player.getName());
            player.sendActionBar(net.kyori.adventure.text.Component.text("§cErreur lors de la création de la mine !"));
            player.sendMessage(getMessage("Messages.mine-creation-failed"));
            player.sendMessage(ColorUtil.deserialize("&cAn error occurred while creating your mine. Contact an admin."));
        }
    }
    /**
     * Téléporte un joueur à sa mine.
     */
    private void teleportPlayerToMine(Player player, Mine mine) {
        Location loc = getBetterTeleportLocation(mine);
        player.teleport(loc);
    }
    /**
     * Retourne la meilleure position de téléportation pour une mine.
     */
    public Location getBetterTeleportLocation(Mine mine) {
        debug("[DEBUG] Calcul du point de téléportation pour la mine de " + 
               (mine.getOwner() != null ? Bukkit.getOfflinePlayer(mine.getOwner()).getName() : "inconnu") + 
               " (UUID: " + mine.getOwner() + ")");
        if (mine.getTeleportLocation() != null && mine.getTeleportLocation().getWorld() != null) {
            debug("[DEBUG] Utilisation du point de téléportation personnalisé");
            return mine.getTeleportLocation();
        }
        if (mine.hasMineArea()) {
            debug("[DEBUG] Calcul d'un point de téléportation basé sur les limites de la mine");
            World world = mine.getLocation().getWorld();
            if (world == null) {
                plugin.getLogger().warning("[DEBUG] Le monde de la mine est null, utilisation de la position de base");
                return mine.getLocation().clone().add(0.5, 1, 0.5);
            }
            debug("[DEBUG] Position de la mine: " + mine.getLocation().getWorld().getName() + 
                   " [" + mine.getLocation().getX() + "," + 
                   mine.getLocation().getY() + "," + 
                   mine.getLocation().getZ() + "]");
            debug("[DEBUG] Zone de la mine: (" + mine.getMinX() + "," + mine.getMinY() + "," + mine.getMinZ() + 
                   ") à (" + mine.getMaxX() + "," + mine.getMaxY() + "," + mine.getMaxZ() + ")");
            int centerZ = (mine.getMinZ() + mine.getMaxZ()) / 2;
            int teleportX = mine.getMinX() - 2; 
            int teleportZ = centerZ; 
            int teleportY = Math.max(64, mine.getMinY() + 1);
            Location teleportLocation = new Location(world, teleportX + 0.5, teleportY, teleportZ + 0.5);
            teleportLocation.setYaw(90); 
            debug("[DEBUG] Point de téléportation calculé: " + teleportLocation.getWorld().getName() + 
                   " [" + teleportLocation.getX() + "," + 
                   teleportLocation.getY() + "," + 
                   teleportLocation.getZ() + "]");
            if (teleportLocation.getBlock().getType() != Material.AIR || 
                teleportLocation.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) {
                debug("[DEBUG] La position calculée n'est pas sûre, recherche d'une position alternative");
                for (int checkY = teleportY; checkY < Math.min(255, teleportY + 20); checkY++) {
                    Location check = new Location(world, teleportX + 0.5, checkY, teleportZ + 0.5);
                    check.setYaw(90); 
                    if (check.getBlock().getType() == Material.AIR && 
                        check.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                        debug("[DEBUG] Position sûre trouvée à Y=" + checkY);
                        return check;
                    }
                }
                plugin.getLogger().warning("[DEBUG] Aucune position sûre trouvée, retour à la position de base");
                Location baseLoc = mine.getLocation().clone().add(0.5, 1, 0.5);
                baseLoc.setYaw(90);
                return baseLoc;
            }
            return teleportLocation;
        }
        else if (mine.hasSchematicBounds()) {
            plugin.getLogger().info("[DEBUG] Utilisation des limites du schéma pour la téléportation");
            double teleportX = mine.getSchematicMinX() - 2;
            double centerY = Math.max(64, mine.getSchematicMinY() + 1);
            double centerZ = (mine.getSchematicMinZ() + mine.getSchematicMaxZ()) / 2;
            Location schematicCenter = new Location(mine.getLocation().getWorld(), teleportX, centerY, centerZ);
            schematicCenter.setYaw(90); 
            plugin.getLogger().info("[DEBUG] Point de téléportation basé sur le schéma: " + schematicCenter.toString());
            return schematicCenter;
        }
        plugin.getLogger().info("[DEBUG] Aucune zone ou schéma défini, utilisation de la position de base de la mine");
        Location baseLoc = mine.getLocation().clone().add(0.5, 1, 0.5);
        baseLoc.setYaw(90); 
        return baseLoc;
    }
    public MineProtectionManager getMineProtectionManager() {
        return protectionManager;
    }
    public void saveMineData(Player player) {
        minePersistenceService.saveMineData(player, this);
    }
    public void saveMine(Mine mine) {
        minePersistenceService.saveMine(mine, this);
    }
    public void loadMineData() {
        minePersistenceService.loadMineData(this, configManager, plugin, protectionManager, mineTiers);
    }
    public void saveAllMineData() {
        minePersistenceService.saveAllMineData(this, configManager, plugin);
    }
    public void resetMine(UUID uuid) {
        mineResetService.resetMine(uuid, this, plugin, mineTiers);
    }
    public void initializeAllMines() {
        plugin.getLogger().info("[DEBUG] Performing post-load initialization of all mines...");
        int count = 0;
        for (Mine mine : mineMemoryService.getAllMines()) {
            if (mine.hasMineArea()) {
                mine.calculateTotalBlocks();
                mine.synchronizeStats();
                count++;
            }
        }
        plugin.getLogger().info("[DEBUG] Post-load initialization completed for " + count + " mines.");
    }
    public void resetMine(Player player) {
        mineResetService.resetMine(player, this, plugin, mineTiers);
    }
    public boolean setMineTax(Player player, int tax) {
        return mineTaxService.setMineTax(player, tax);
    }
    public boolean pregenMines(Player player, int count, String type) {
        return minePregenService.pregenMines(player, count, type);
    }
    public boolean deleteMine(Player player) {
        return mineDeleteService.deleteMine(player);
    }
    public boolean upgradeMine(Player player) {
        return mineUpgradeService.upgradeMine(player);
    }
    public boolean expandMine(Player player) {
        return mineUpgradeService.expandMine(player);
    }
    public boolean expandMine(Player player, int expandSize) {
        return mineUpgradeService.expandMine(player, expandSize);
    }
    /**
     * Téléporte un visiteur à la mine d'un propriétaire. Retourne true si succès.
     */
    public boolean teleportToMine(Player owner, Player visitor) {
        if (!hasMine(owner)) {
            owner.sendMessage(getMessage("Messages.no-mine"));
            return false;
        }
        Optional<Mine> mineOpt = getMine(owner);
        if (mineOpt.isEmpty()) {
            owner.sendMessage(ColorUtil.deserialize("&cErreur lors de la récupération de votre mine."));
            return false;
        }
        teleportPlayerToMine(visitor, mineOpt.get());
        visitor.sendMessage(ColorUtil.deserialize("&aVous avez été téléporté à la mine de " + owner.getName() + "."));
        return true;
    }
    public void addMineToMap(UUID uuid, Mine mine) {
        mineMemoryService.addMineToMap(uuid, mine);
    }
    public void removeMine(UUID uuid) {
        mineMemoryService.removeMine(uuid);
    }
    public Mine findAvailablePregenMine() {
        for (Map.Entry<UUID, Mine> entry : mineMemoryService.getPlayerMines().entrySet()) {
            Mine mine = entry.getValue();
            if (plugin.getServer().getPlayer(entry.getKey()) == null) {
                return mine;
            }
        }
        return null;
    }
    public boolean assignPregenMineToPlayer(Mine pregenMine, Player player) {
        if (pregenMine == null || player == null) return false;
        mineMemoryService.removeMine(pregenMine.getOwner());
        pregenMine.setLocation(player.getLocation()); 
        try {
            pregenMine.setOwner(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("Impossible de changer l'owner de la mine pré-générée : " + e.getMessage());
            return false;
        }
        mineMemoryService.addMineToMap(player.getUniqueId(), pregenMine);
        saveMineData(player);
        return true;
    }
    public Map<Integer, Map<org.bukkit.Material, Double>> getMineTiers() {
        return mineTiers;
    }
    public MineGenerationService getMineGenerationService() {
        return mineGenerationService;
    }
    private Title createMineCreatedTitle(Player player) {
        return Title.title(
            Component.text(ColorUtil.translateColors(configManager.getMessage("Messages.titles.mine-created.title"))),
            Component.text(ColorUtil.translateColors(configManager.getMessage("Messages.titles.mine-created.subtitle"))),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );
    }
} 