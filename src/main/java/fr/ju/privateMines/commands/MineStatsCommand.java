package fr.ju.privateMines.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.commands.utils.MineCommandUtils;
import fr.ju.privateMines.guis.MineStatsGUI;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;

public class MineStatsCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    private final PrivateMines plugin;

    public MineStatsCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        this.mineManager = mineManager;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("top")) {
                showTopStats(player);
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(configManager.getMessage("mine-invalid-player"));
                return true;
            }
            showPlayerStats(player, target.getUniqueId());
        } else {
            showPlayerStats(player, player.getUniqueId());
        }
        return true;
    }

    private void showPlayerStats(Player viewer, UUID ownerUUID) {
        MineStatsGUI.openGUI(viewer);
    }

    private void showTopStats(Player viewer) {
        if (plugin.getStatsManager() != null) {
            MineCommandUtils.showTopStats(plugin, mineManager, viewer);
        } else {
            viewer.sendMessage(configManager.getMessage("mine-stats-not-enabled"));
        }
    }
} 