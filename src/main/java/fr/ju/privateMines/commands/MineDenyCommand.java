package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineDenyCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineDenyCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player owner, String[] args, CommandSender sender, Command command, String label) {
        if (!owner.hasPermission(Permissions.DENY)) {
            owner.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            owner.sendMessage("§cUsage: /" + label + " deny <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            owner.sendMessage(configManager.getMessage("Messages.invalid-player"));
            return true;
        }
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(configManager.getMessage("Messages.cannot-deny-self"));
            return true;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        mine.denyAccess(target.getUniqueId());
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(configManager.getMessage("Messages.player-denied", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(configManager.getMessage("Messages.you-were-denied", targetReplacements));
        return true;
    }
} 