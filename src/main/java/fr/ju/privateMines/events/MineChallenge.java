package fr.ju.privateMines.events;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
public class MineChallenge {
    private String id;
    private String name;
    private String description;
    private int requiredAmount;
    private ChallengeType type;
    private ItemStack reward;
    private Consumer<Player> customRewardAction;
    public MineChallenge(String id, String name, String description, int requiredAmount, ChallengeType type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredAmount = requiredAmount;
        this.type = type;
    }
    public void setItemReward(Material material, int amount) {
        this.reward = new ItemStack(material, amount);
    }
    public void setCustomReward(Consumer<Player> action) {
        this.customRewardAction = action;
    }
    public void giveReward(Player player) {
        if (reward != null) {
            player.getInventory().addItem(reward);
        }
        if (customRewardAction != null) {
            customRewardAction.accept(player);
        }
        player.sendMessage(PrivateMines.getInstance().getConfigManager().getMessageOrDefault("challenge.completed", "§aYou have completed the challenge §6%name% §aand received your reward!").replace("%name%", name));
    }
    public boolean matchesAction(ChallengeType actionType) {
        return this.type == actionType;
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
    public int getRequiredAmount() {
        return requiredAmount;
    }
    public ChallengeType getType() {
        return type;
    }
    public enum ChallengeType {
        MINE_BLOCKS,      
        MINE_SPECIFIC,    
        VISIT_MINES,      
        RESET_MINES,      
        UPGRADE_MINE      
    }
} 