package fr.ju.privateMines.commands;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;

public class MineResetCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineResetCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.RESET)) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(configManager.getMessage("no-mine"));
            return true;
        }
        if (mine.hasMineArea()) {
            int centerX = (mine.getMinX() + mine.getMaxX()) / 2;
            int centerZ = (mine.getMinZ() + mine.getMaxZ()) / 2;
            int tpX = centerX - 10;
            int tpY = 64;
            int tpZ = centerZ;
            Location tpLoc = new Location(mine.getLocation().getWorld(), tpX + 0.5, tpY, tpZ + 0.5);
            tpLoc.setYaw(270);
            mine.setTeleportLocation(tpLoc);
            mineManager.saveMineData(player);
            player.teleport(tpLoc);
            player.sendMessage(ColorUtil.deserialize("&aVous avez été téléporté à l'entrée de votre mine."));
        }
        mineManager.resetMine(player);
        return true;
    }
} 