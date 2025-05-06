package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.MemberCommandUtils;

public class MineRemoveCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;

    public MineRemoveCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        Object[] result = MemberCommandUtils.checkMemberCommandConditions(
            player, args, mineManager, configManager, "mine-usage-remove");
        
        if (result == null) {
            return true;
        }
        
        Mine mine = (Mine) result[0];
        Player target = (Player) result[1];
        
        // Retirer le joueur des membres autorisés
        mine.getMineAccess().removeMember(target.getUniqueId());
        
        // Retirer le joueur de la région WorldGuard
        mineManager.getMineProtectionManager().removeMemberFromMineRegion(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(configManager.getMessage("mine-player-removed")
                .replace("%player%", target.getName()));
        
        return true;
    }
} 