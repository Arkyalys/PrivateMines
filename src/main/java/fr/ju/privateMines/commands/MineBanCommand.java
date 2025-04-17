package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineBanCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineBanCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    private long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return -1;
        if (durationStr.matches("\\d+")) return Long.parseLong(durationStr);
        long totalSeconds = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([dhms])");
        java.util.regex.Matcher matcher = pattern.matcher(durationStr.toLowerCase());
        boolean found = false;
        while (matcher.find()) {
            found = true;
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d": totalSeconds += amount * 86400; break;
                case "h": totalSeconds += amount * 3600; break;
                case "m": totalSeconds += amount * 60; break;
                case "s": totalSeconds += amount; break;
            }
        }
        return found ? totalSeconds : -1;
    }
    @Override
    public boolean execute(Player owner, String[] args, CommandSender sender, Command command, String label) {
        if (!owner.hasPermission(Permissions.BAN)) {
            owner.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            owner.sendMessage(ColorUtil.deserialize("&cUsage: /" + label + " ban <player> [duration]"));
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
            owner.sendMessage(configManager.getMessage("Messages.cannot-ban-self"));
            return true;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        if (args.length > 2) {
            long duration = parseDuration(args[2]);
            if (duration <= 0) {
                owner.sendMessage(ColorUtil.deserialize("&cDurée invalide. Utilisez le format: 1d2h3m4s"));
                return true;
            }
            mine.banPlayer(target.getUniqueId(), duration);
        } else {
            mine.banPlayerPermanently(target.getUniqueId());
        }
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(configManager.getMessage("Messages.player-banned", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(configManager.getMessage("Messages.you-were-banned", targetReplacements));
        return true;
    }
} 