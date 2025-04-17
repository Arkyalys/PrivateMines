package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineAllowCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineAllowCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player owner, String[] args, CommandSender sender, Command command, String label) {
        if (!owner.hasPermission(Permissions.ALLOW)) {
            owner.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            owner.sendMessage("§cUsage: /" + label + " allow <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                owner.sendMessage(configManager.getMessage("Messages.invalid-player"));
                return true;
            }
        }
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        if (!mine.getMineAccess().isDenied(target.getUniqueId())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
            owner.sendMessage(configManager.getMessage("Messages.player-not-denied", replacements));
            return true;
        }
        mine.allowAccess(target.getUniqueId());
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
        owner.sendMessage(configManager.getMessage("Messages.player-allowed", ownerReplacements));
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("%owner%", owner.getName());
            onlineTarget.sendMessage(configManager.getMessage("Messages.you-were-allowed", targetReplacements));
        }
        return true;
    }
} 