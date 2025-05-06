package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.MemberCommandUtils;

public class MineAddCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;

    public MineAddCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        Object[] result = MemberCommandUtils.checkMemberCommandConditions(
            player, args, mineManager, configManager, "mine-usage-add");
        
        if (result == null) {
            return true;
        }
        
        Mine mine = (Mine) result[0];
        Player target = (Player) result[1];
        
        // Ajouter le joueur à la liste des membres
        mine.getMineAccess().addMember(target.getUniqueId());
        
        // Ajouter le joueur à la région WorldGuard
        mineManager.getMineProtectionManager().addMemberToMineRegion(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(configManager.getMessage("mine-player-added")
                .replace("%player%", target.getName()));
        
        return true;
    }
} 