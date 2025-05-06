package fr.ju.privateMines.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;

public class MineRemoveCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;

    public MineRemoveCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("mine-usage-remove"));
            return true;
        }
        
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(configManager.getMessage("mine-not-found"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(configManager.getMessage("mine-invalid-player"));
            return true;
        }
        
        // Retirer le joueur des membres autorisés
        mine.getMineAccess().removeMember(target.getUniqueId());
        
        // Retirer le joueur de la région WorldGuard
        mineManager.getMineProtectionManager().removeMemberFromMineRegion(player.getUniqueId(), target.getUniqueId());
        
        player.sendMessage(configManager.getMessage("mine-player-removed")
                .replace("%player%", target.getName()));
        
        return true;
    }
} 