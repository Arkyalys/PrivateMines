package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MinePregenCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;

    public MinePregenCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN_PREGEN)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("mine-usage-pregen"));
            return true;
        }
        
        try {
            int count = Integer.parseInt(args[1]);
            String type = args.length > 2 ? args[2] : "default";
            mineManager.pregenMines(player, count, type);
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getMessage("mine-invalid-number"));
        }
        
        return true;
    }
} 