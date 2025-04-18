package fr.ju.privateMines.commands;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineTeleportCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineTeleportCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.TELEPORT)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine != null) {
            Location tpLocation = mineManager.getBetterTeleportLocation(mine);
            if (tpLocation == null) {
                player.sendMessage(configManager.getMessage("mine-teleport-error"));
                return true;
            }
            player.teleport(tpLocation);
            player.sendMessage(configManager.getMessage("mine-teleport"));
            mineManager.saveMineData(player);
        } else {
            player.sendMessage(configManager.getMessage("mine-no-mine"));
        }
        return true;
    }
} 