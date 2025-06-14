package fr.ju.privateMines.events;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.events.MineChallenge.ChallengeType;
import fr.ju.privateMines.utils.ColorUtil;
public class EventManager {
    private final PrivateMines plugin;
    private List<MineEvent> events;
    private Map<String, MineEvent> eventById;
    private BukkitTask checkTask;
    private BukkitTask particleTask;
    public EventManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.events = new ArrayList<>();
        this.eventById = new HashMap<>();
        loadEvents();
        startScheduler();
    }
    public void loadEvents() {
        events.clear();
        eventById.clear();
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection eventsSection = config.getConfigurationSection("Events");
        if (eventsSection == null) {
            createDemoEvent();
            return;
        }
        for (String eventId : eventsSection.getKeys(false)) {
            ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
            if (eventSection == null) continue;
            MineEvent event = createMineEventFromSection(eventId, eventSection);
            events.add(event);
            eventById.put(eventId, event);
        }
        plugin.getLogger().info("Chargement de " + events.size() + " événements programmés");
    }
    private MineEvent createMineEventFromSection(String eventId, ConfigurationSection eventSection) {
        String name = eventSection.getString("name", "Événement de mine");
        String description = eventSection.getString("description", "Un événement spécial pour les mines");
        long startTime = eventSection.getLong("start-time", System.currentTimeMillis());
        long endTime = eventSection.getLong("end-time", System.currentTimeMillis() + 86400000);
        MineEvent event = new MineEvent(eventId, name, description, startTime, endTime);
        loadBlockBoosts(event, eventSection.getConfigurationSection("block-boosts"));
        loadChallenges(event, eventSection.getConfigurationSection("challenges"));
        return event;
    }
    private void loadBlockBoosts(MineEvent event, ConfigurationSection boostsSection) {
        if (boostsSection != null) {
            for (String materialName : boostsSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    double boost = boostsSection.getDouble(materialName, 1.0);
                    event.addBlockBoost(material, boost);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Matériau inconnu dans la configuration des événements: " + materialName);
                }
            }
        }
    }
    private void loadChallenges(MineEvent event, ConfigurationSection challengesSection) {
        if (challengesSection != null) {
            for (String challengeId : challengesSection.getKeys(false)) {
                ConfigurationSection challengeSection = challengesSection.getConfigurationSection(challengeId);
                if (challengeSection == null) continue;
                MineChallenge challenge = createChallengeFromSection(challengeId, challengeSection);
                if (challenge != null) {
                    event.addChallenge(challenge);
                }
            }
        }
    }
    private MineChallenge createChallengeFromSection(String challengeId, ConfigurationSection challengeSection) {
        String challengeName = challengeSection.getString("name", "Défi");
        String challengeDesc = challengeSection.getString("description", "Un défi spécial");
        int requiredAmount = challengeSection.getInt("required-amount", 100);
        String typeStr = challengeSection.getString("type", "MINE_BLOCKS");
        ChallengeType type;
        try {
            type = ChallengeType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Type de défi inconnu: " + typeStr);
            return null;
        }
        MineChallenge challenge = new MineChallenge(challengeId, challengeName, challengeDesc, requiredAmount, type);
        String rewardItem = challengeSection.getString("reward.item");
        int rewardAmount = challengeSection.getInt("reward.amount", 1);
        if (rewardItem != null) {
            try {
                Material rewardMaterial = Material.valueOf(rewardItem.toUpperCase());
                challenge.setItemReward(rewardMaterial, rewardAmount);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Matériau de récompense inconnu: " + rewardItem);
            }
        }
        return challenge;
    }
    private void createDemoEvent() {
        long now = System.currentTimeMillis();
        long oneDay = 86400000;
        MineEvent demoEvent = new MineEvent("demo", "Événement de démonstration", 
                "Un événement spécial avec des récompenses exclusives !", now, now + oneDay);
        demoEvent.addBlockBoost(Material.DIAMOND_ORE, 2.0);
        demoEvent.addBlockBoost(Material.EMERALD_ORE, 1.5);
        MineChallenge challenge1 = new MineChallenge("mine_blocks", "Mineur Infatigable", 
                "Miner 500 blocs dans votre mine.", 500, ChallengeType.MINE_BLOCKS);
        challenge1.setItemReward(Material.DIAMOND, 5);
        MineChallenge challenge2 = new MineChallenge("reset_mine", "Reset Pro", 
                "Réinitialiser votre mine 5 fois.", 5, ChallengeType.RESET_MINES);
        challenge2.setItemReward(Material.EMERALD, 3);
        demoEvent.addChallenge(challenge1);
        demoEvent.addChallenge(challenge2);
        events.add(demoEvent);
        eventById.put("demo", demoEvent);
        plugin.getLogger().info("Création d'un événement de démonstration (aucun événement configuré)");
    }
    private void startScheduler() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (MineEvent event : events) {
                if (event.isActive()) {
                    plugin.getServer().getOnlinePlayers().forEach(player ->
                        player.sendMessage(plugin.getConfigManager().getMessageOrDefault("event.active", "&6[PrivateMines] &aL'événement &e%event% &aest actif !").replace("%event%", event.getName()))
                    );
                }
            }
        }, 20L, 6000L); 
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateParticles, 1L, 5L);
    }
    private void updateParticles() {
    }
    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
    }
    public void recordAction(Player player, ChallengeType type, int amount) {
        for (MineEvent event : events) {
            if (event.isActive()) {
                for (MineChallenge challenge : event.getChallenges()) {
                    if (challenge.matchesAction(type)) {
                        event.addProgress(player, challenge.getId(), amount);
                    }
                }
            }
        }
    }
    public List<MineEvent> getActiveEvents() {
        List<MineEvent> activeEvents = new ArrayList<>();
        for (MineEvent event : events) {
            if (event.isActive()) {
                activeEvents.add(event);
            }
        }
        return activeEvents;
    }
    public MineEvent getEvent(String eventId) {
        return eventById.get(eventId);
    }
} 