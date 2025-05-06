package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MineReloadCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    private final PrivateMines plugin;

    public MineReloadCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.mineManager = mineManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN_RELOAD)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        
        player.sendMessage(configManager.getMessage("mine-reload-start"));
        if (plugin.reloadPlugin()) {
            player.sendMessage(configManager.getMessage("mine-reload-success"));
        } else {
            player.sendMessage(configManager.getMessage("mine-reload-failed"));
        }
        
        return true;
    }
} 