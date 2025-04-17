package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
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
            player.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.deserialize("&cUtilisation: /" + label + " visit <joueur>"));
            return true;
        }
        Player targetPlayer = player.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(configManager.getMessage("Messages.invalid-player"));
            return true;
        }
        if (!mineManager.hasMine(targetPlayer)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", targetPlayer.getName());
            player.sendMessage(configManager.getMessage("Messages.no-other-mine", replacements));
            return true;
        }
        Mine targetMine = mineManager.getMine(targetPlayer).orElse(null);
        if (targetMine == null) {
            player.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        if (!targetMine.isOpen()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", targetPlayer.getName());
            player.sendMessage(configManager.getMessage("Messages.mine-closed", replacements));
            return true;
        }
        if (!targetMine.canPlayerAccess(player.getUniqueId())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", targetPlayer.getName());
            if (targetMine.getMineAccess().isBanned(player.getUniqueId())) {
                player.sendMessage(configManager.getMessage("Messages.you-are-banned", replacements));
            } else {
                player.sendMessage(configManager.getMessage("Messages.you-are-denied", replacements));
            }
            return true;
        }
        Location targetLocation = mineManager.getBetterTeleportLocation(targetMine);
        if (targetLocation == null) {
            player.sendMessage(ColorUtil.deserialize("&cImpossible de déterminer un point de téléportation sûr. Contactez un administrateur."));
            return true;
        }
        player.teleport(targetLocation);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%player%", targetPlayer.getName());
        player.sendMessage(configManager.getMessage("Messages.teleported-to-other-mine", replacements));
        if (targetPlayer.isOnline()) {
            Map<String, String> ownerReplacements = new HashMap<>();
            ownerReplacements.put("%player%", player.getName());
            targetPlayer.sendMessage(configManager.getMessage("Messages.player-visiting-mine", ownerReplacements));
        }
        try {
            targetMine.addVisit(player.getUniqueId());
        } catch (Exception ignored) {}
        return true;
    }
} 