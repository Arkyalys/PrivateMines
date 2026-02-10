package fr.ju.privateMines.managers;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.services.IStatsService;
import fr.ju.privateMines.services.MineMemoryService;
import fr.ju.privateMines.services.MinePersistenceService;
import fr.ju.privateMines.services.StatsServiceAdapter;
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
    private final MineTeleportService mineTeleportService;
    public final MineMemoryService mineMemoryService;
    private final IStatsService statsService;
    public MineManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.protectionManager = new MineProtectionManager(plugin);
        this.mineTiers = new HashMap<>();
        
        // Création de l'adaptateur pour le service de statistiques
        if (plugin.getStatsManager() != null) {
            this.statsService = new StatsServiceAdapter(plugin.getStatsManager());
        } else {
            this.statsService = null;
        }
        
        MineAreaDetector areaDetector = new MineAreaDetector(plugin);
        SchematicManager schematicManager = new SchematicManager(plugin);
        this.mineGenerationService = new MineGenerationService(plugin, protectionManager, areaDetector, schematicManager);
        this.mineResetService = new MineResetService(plugin);
        this.mineTaxService = new MineTaxService(plugin, this);
        this.mineDeleteService = new MineDeleteService(plugin, this);
        this.mineUpgradeService = new MineUpgradeService(plugin, this);
        this.minePregenService = new MinePregenService(plugin, this);
        this.minePersistenceService = new MinePersistenceService(plugin);
        this.mineTeleportService = new MineTeleportService(plugin);
        this.mineMemoryService = new MineMemoryService();
        loadMineTiers();
        loadMineData();
    }
    /**
     * Charge les tiers de mines depuis le fichier de configuration.
     * Cette méthode a été refactorisée pour réduire sa complexité cyclomatique en
     * déléguant les différentes étapes à des méthodes d'assistance spécialisées :
     * - validateTiersConfig : vérifie la validité de base du fichier de configuration
     * - getTiersSection : extrait la section de configuration des tiers
     * - processTiersSections : traite chaque section de tier
     * - processBlocksForTier : traite les blocs pour un tier spécifique
     * - loadBlocksForTier : charge les types de blocs et leur probabilité
     * - logTierLoaded : journalise les informations de chargement
     */
    public void loadMineTiers() {
        mineTiers.clear();
        plugin.getLogger().info("Chargement des tiers de mines...");
        
        org.bukkit.configuration.file.FileConfiguration tiersConfig = plugin.getConfigManager().getTiersConfig();
        if (!validateTiersConfig(tiersConfig)) {
            return;
        }
        
        org.bukkit.configuration.ConfigurationSection tiersSection = getTiersSection(tiersConfig);
        if (tiersSection == null) {
            return;
        }
        
        processTiersSections(tiersSection);
        
        plugin.getLogger().info(mineTiers.size() + " tiers de mines chargés");
    }
    
    /**
     * Valide le fichier de configuration des tiers
     * @return true si la configuration est valide, false sinon
     */
    private boolean validateTiersConfig(org.bukkit.configuration.file.FileConfiguration tiersConfig) {
        if (tiersConfig == null) {
            plugin.getLogger().warning("Erreur: Impossible de charger le fichier tiers.yml");
            return false;
        }
        
        if (!tiersConfig.isConfigurationSection("tiers")) {
            plugin.getLogger().warning("Erreur: La section 'tiers' n'existe pas dans tiers.yml");
            return false;
        }
        
        return true;
    }
    
    /**
     * Récupère la section de configuration des tiers
     * @return la section ou null si elle est invalide
     */
    private org.bukkit.configuration.ConfigurationSection getTiersSection(org.bukkit.configuration.file.FileConfiguration tiersConfig) {
        org.bukkit.configuration.ConfigurationSection tiersSection = tiersConfig.getConfigurationSection("tiers");
        if (tiersSection == null) {
            plugin.getLogger().warning("Erreur: La section 'tiers' est invalide dans tiers.yml");
            return null;
        }
        return tiersSection;
    }
    
    /**
     * Traite toutes les sections de tiers
     */
    private void processTiersSections(org.bukkit.configuration.ConfigurationSection tiersSection) {
        for (String tierKey : tiersSection.getKeys(false)) {
            try {
                int tier = Integer.parseInt(tierKey);
                processBlocksForTier(tiersSection, tierKey, tier);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Erreur: Tier invalide '" + tierKey + "' dans tiers.yml");
            }
        }
    }
    
    /**
     * Traite les blocs pour un tier spécifique
     */
    private void processBlocksForTier(org.bukkit.configuration.ConfigurationSection tiersSection, String tierKey, int tier) {
        org.bukkit.configuration.ConfigurationSection blocksSection = tiersSection.getConfigurationSection(tierKey + ".blocks");
        if (blocksSection == null) {
            return;
        }
        
        Map<Material, Double> blocks = loadBlocksForTier(blocksSection, tier);
        
        if (!blocks.isEmpty()) {
            mineTiers.put(tier, blocks);
            logTierLoaded(tier, blocks);
        }
    }
    
    /**
     * Charge les types de blocs et leurs probabilités pour un tier
     */
    private Map<Material, Double> loadBlocksForTier(org.bukkit.configuration.ConfigurationSection blocksSection, int tier) {
        Map<Material, Double> blocks = new HashMap<>();
        
        for (String blockKey : blocksSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(blockKey);
                double chance = blocksSection.getDouble(blockKey);
                blocks.put(material, chance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Erreur: Matériau invalide '" + blockKey + "' dans le tier " + tier);
            }
        }
        
        return blocks;
    }
    
    /**
     * Journalise l'information sur un tier chargé (en mode debug)
     */
    private void logTierLoaded(int tier, Map<Material, Double> blocks) {
        if (PrivateMines.isDebugMode()) {
            plugin.getLogger().info("Tier " + tier + " chargé avec " + blocks.size() + " blocs");
        }
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
            player.sendMessage(getMessage("already-own-mine"));
            return false;
        }
        
        // Création de la mine avec injection du service de statistiques
        Mine mine = new Mine(player.getUniqueId(), location);
        mine.setStatsService(statsService);
        
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
            player.sendMessage(getMessage("no-available-location"));
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
            player.sendMessage(getMessage("mine-created"));
            player.sendMessage(getMessage("teleported-to-mine-after-creation"));
            teleportPlayerToMine(player, mine);
            Title title = createMineCreatedTitle(player);
            player.showTitle(title);
        } else {
            plugin.getLogger().warning("Échec de la génération de la mine pour " + player.getName());
            player.sendActionBar(net.kyori.adventure.text.Component.text("§cErreur lors de la création de la mine !"));
            player.sendMessage(getMessage("mine-creation-failed"));
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("mine.creation-error", "&cAn error occurred while creating your mine. Contact an admin."));
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
     * Délègue au service MineTeleportService.
     */
    public Location getBetterTeleportLocation(Mine mine) {
        return mineTeleportService.getBetterTeleportLocation(mine);
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
        PrivateMines.debugLog("Performing post-load initialization of all mines...");
        int count = 0;
        for (Mine mine : mineMemoryService.getAllMines()) {
            // Injecter le service de statistiques à chaque mine
            mine.setStatsService(statsService);
            
            if (mine.hasMineArea()) {
                mine.calculateTotalBlocks();
                mine.synchronizeStats();
                count++;
            }
        }
        PrivateMines.debugLog("Post-load initialization completed for " + count + " mines.");
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
            owner.sendMessage(getMessage("no-mine"));
            return false;
        }
        Optional<Mine> mineOpt = getMine(owner);
        if (mineOpt.isEmpty()) {
            owner.sendMessage(plugin.getConfigManager().getMessageOrDefault("gui.mine-error", "&cErreur lors de la récupération de votre mine."));
            return false;
        }
        
        Mine mine = mineOpt.get();
        boolean success = mineTeleportService.teleportToMine(owner, visitor, mine);
        
        if (success) {
            visitor.sendMessage(plugin.getConfigManager().getMessageOrDefault("mine.visit-success", "&aVous avez été téléporté à la mine de %player%." ).replace("%player%", owner.getName()));
        }
        
        return success;
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
            Component.text(ColorUtil.translateColors(configManager.getMessage("titles.mine-created.title"))),
            Component.text(ColorUtil.translateColors(configManager.getMessage("titles.mine-created.subtitle"))),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        );
    }
    /**
     * Récupère le service de statistiques
     */
    public IStatsService getStatsService() {
        return statsService;
    }
} 