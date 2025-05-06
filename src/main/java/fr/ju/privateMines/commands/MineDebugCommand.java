package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MineDebugCommand implements SubCommand {
    private final ConfigManager configManager;

    public MineDebugCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        
        boolean newState = !PrivateMines.isDebugMode();
        PrivateMines.setDebugMode(newState);
        player.sendMessage(configManager.getMessage(newState ? "mine-debug-enabled" : "mine-debug-disabled"));
        
        return true;
    }
} 