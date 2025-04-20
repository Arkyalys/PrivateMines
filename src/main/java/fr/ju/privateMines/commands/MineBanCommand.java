package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.commands.utils.MineCommandUtils;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineBanCommand implements SubCommand {
    private final MineManager mineManager;
    public MineBanCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
    }
    @Override
    public boolean execute(Player owner, String[] args, CommandSender sender, Command command, String label) {
        if (!owner.hasPermission(Permissions.BAN)) {
            owner.sendMessage(ColorUtil.deserialize("&cYou don't have permission to use this command!"));
            return true;
        }
        if (args.length < 2) {
            owner.sendMessage(ColorUtil.deserialize("&cUsage: /" + label + " ban <player> [duration]"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            owner.sendMessage(ColorUtil.deserialize("&cInvalid player!"));
            return true;
        }
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(ColorUtil.deserialize("&cYou do not own a mine."));
            return true;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(ColorUtil.deserialize("&cYou cannot ban yourself!"));
            return true;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(ColorUtil.deserialize("&cYou do not own a mine."));
            return true;
        }
        if (args.length > 2) {
            long duration = MineCommandUtils.parseDuration(args[2]);
            if (duration <= 0) {
                owner.sendMessage(ColorUtil.deserialize("&cInvalid duration. Use format: 1d2h3m4s"));
                return true;
            }
            mine.banPlayer(target.getUniqueId(), duration);
        } else {
            mine.banPlayerPermanently(target.getUniqueId());
        }
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(ColorUtil.deserialize("&aPlayer &e" + target.getName() + " &ahas been banned from your mine."));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(ColorUtil.deserialize("&cYou have been banned from the mine of &e" + owner.getName() + "&c!"));
        return true;
    }
} 