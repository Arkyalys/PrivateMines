package fr.ju.privateMines.commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineSetTierCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineSetTierCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.ADMIN_SET_TIER)) {
            player.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ColorUtil.translateColors("&cUsage: /jumine settier <player> <level>"));
            return true;
        }
        Player tierTargetPlayer = player.getServer().getPlayer(args[1]);
        if (tierTargetPlayer == null) {
            player.sendMessage(configManager.getMessage("Messages.invalid-player"));
            return true;
        }
        try {
            int tier = Integer.parseInt(args[2]);
            if (tier <= 0) {
                player.sendMessage(ColorUtil.translateColors("&cThe level must be greater than 0"));
                return true;
            }
            Mine mine = mineManager.getMine(tierTargetPlayer.getUniqueId()).orElse(null);
            if (mine == null) {
                player.sendMessage(configManager.getMessage("Messages.no-mine"));
                return true;
            }
            mine.setTier(tier);
            mineManager.resetMine(tierTargetPlayer);
            player.sendMessage(configManager.getMessage("Messages.mine-tier-set"));
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.translateColors("&cThe level must be a valid number"));
        }
        return true;
    }
} 