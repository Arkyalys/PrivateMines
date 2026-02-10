package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
import fr.ju.privateMines.PrivateMines;

public class MineVisitCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineVisitCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.TELEPORT)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("mine-usage-visit"));
            return true;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(configManager.getMessage("mine-invalid-player"));
            return true;
        }
        Mine targetMine = mineManager.getMine(targetPlayer).orElse(null);
        if (targetMine == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", targetPlayer.getName());
            player.sendMessage(configManager.getMessage("mine-no-other-mine", replacements));
            return true;
        }
        if (!targetMine.isOpen() && !targetMine.getOwner().equals(player.getUniqueId())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", targetPlayer.getName());
            player.sendMessage(configManager.getMessage("mine-closed", replacements));
            return true;
        }
        Location targetLocation = mineManager.getBetterTeleportLocation(targetMine);
        if (targetLocation == null) {
            player.sendMessage(configManager.getMessage("mine-teleport-error"));
            return true;
        }
        player.teleport(targetLocation);
        targetMine.getStats().addVisit(player.getUniqueId());
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%player%", targetPlayer.getName());
        player.sendMessage(configManager.getMessage("mine-visit-success", replacements));
        replacements = new HashMap<>();
        replacements.put("%player%", player.getName());
        targetPlayer.sendMessage(configManager.getMessage("mine-player-visiting-mine", replacements));
        return true;
    }
} 