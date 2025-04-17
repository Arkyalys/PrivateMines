package fr.ju.privateMines.events;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
public class MineEvent {
    private String id;
    private String name;
    private String description;
    private long startTime;
    private long endTime;
    private Map<Material, Double> blockBoosts;
    private List<MineChallenge> challenges;
    private Map<UUID, Map<String, Integer>> playerProgress;
    public MineEvent(String id, String name, String description, long startTime, long endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.blockBoosts = new HashMap<>();
        this.challenges = new ArrayList<>();
        this.playerProgress = new HashMap<>();
    }
    public void addBlockBoost(Material material, double boost) {
        blockBoosts.put(material, boost);
    }
    public void addChallenge(MineChallenge challenge) {
        challenges.add(challenge);
    }
    public boolean isActive() {
        long now = System.currentTimeMillis();
        return now >= startTime && now <= endTime;
    }
    public void addProgress(Player player, String challengeId, int amount) {
        UUID uuid = player.getUniqueId();
        playerProgress.putIfAbsent(uuid, new HashMap<>());
        Map<String, Integer> progress = playerProgress.get(uuid);
        progress.put(challengeId, progress.getOrDefault(challengeId, 0) + amount);
        for (MineChallenge challenge : challenges) {
            if (challenge.getId().equals(challengeId)) {
                if (progress.getOrDefault(challengeId, 0) >= challenge.getRequiredAmount()) {
                    challenge.giveReward(player);
                }
                break;
            }
        }
    }
    public double getBlockBoost(Material material) {
        return blockBoosts.getOrDefault(material, 1.0);
    }
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public long getStartTime() {
        return startTime;
    }
    public long getEndTime() {
        return endTime;
    }
    public List<MineChallenge> getChallenges() {
        return new ArrayList<>(challenges);
    }
    public Map<UUID, Map<String, Integer>> getPlayerProgress() {
        return new HashMap<>(playerProgress);
    }
} 