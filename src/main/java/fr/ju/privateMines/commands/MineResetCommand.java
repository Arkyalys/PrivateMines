package fr.ju.privateMines.commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MineResetCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineResetCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.RESET)) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(configManager.getMessage("no-mine"));
            return true;
        }
        mineManager.resetMine(player);
        return true;
    }
}
