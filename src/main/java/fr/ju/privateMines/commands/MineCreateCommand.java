package fr.ju.privateMines.commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineCreateCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineCreateCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.CREATE)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        String mineType = "default";
        if (args.length >= 2 && player.hasPermission(Permissions.ADMIN_CREATE)) {
            mineType = args[1];
        }
        mineManager.createMine(player, mineType);
        return true;
    }
} 