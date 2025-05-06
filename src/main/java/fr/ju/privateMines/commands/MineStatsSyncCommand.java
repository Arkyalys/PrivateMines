package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MineStatsSyncCommand implements SubCommand {
    private final ConfigManager configManager;
    private final PrivateMines plugin;

    public MineStatsSyncCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        
        if (args.length > 1) {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target != null) {
                Mine mine = plugin.getMineManager().getMine(target).orElse(null);
                if (mine != null && plugin.getStatsManager() != null) {
                    plugin.getStatsManager().syncMineStats(mine);
                    player.sendMessage(configManager.getMessage("mine-stats-synced")
                            .replace("%player%", target.getName()));
                } else {
                    player.sendMessage(configManager.getMessage("mine-stats-error")
                            .replace("%player%", target.getName()));
                }
            } else {
                player.sendMessage(configManager.getMessage("mine-invalid-player"));
            }
        } else {
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine != null && plugin.getStatsManager() != null) {
                plugin.getStatsManager().syncMineStats(mine);
                player.sendMessage(configManager.getMessage("mine-stats-synced-self"));
            } else {
                player.sendMessage(configManager.getMessage("mine-stats-error-self"));
            }
        }
        
        return true;
    }
} 