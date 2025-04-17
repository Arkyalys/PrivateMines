package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineUpgradeCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineUpgradeCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN_UPGRADE)) {
            player.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        if (mineManager.upgradeMine(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%tier%", String.valueOf(mine.getTier()));
            player.sendMessage(configManager.getMessage("Messages.mine-upgraded", replacements));
        } else {
            player.sendMessage(configManager.getMessage("Messages.upgrade-failed"));
        }
        return true;
    }
} 