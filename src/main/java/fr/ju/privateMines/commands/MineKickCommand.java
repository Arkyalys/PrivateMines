package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineKickCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineKickCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player owner, String[] args, CommandSender sender, Command command, String label) {
        if (!owner.hasPermission(Permissions.KICK)) {
            owner.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        if (args.length < 2) {
            owner.sendMessage("§cUsage: /" + label + " kick <player>");
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
            owner.sendMessage(configManager.getMessage("Messages.cannot-kick-self"));
            return true;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return true;
        }
        World mineWorld = mine.getLocation().getWorld();
        if (!target.getWorld().equals(mineWorld)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName());
            owner.sendMessage(configManager.getMessage("Messages.player-not-in-mine", replacements));
            return true;
        }
        boolean inMine = false;
        if (mine.hasMineArea()) {
            int x = target.getLocation().getBlockX();
            int y = target.getLocation().getBlockY();
            int z = target.getLocation().getBlockZ();
            inMine = x >= mine.getMinX() && x <= mine.getMaxX() && y >= mine.getMinY() && y <= mine.getMaxY() && z >= mine.getMinZ() && z <= mine.getMaxZ();
        } else if (mine.hasSchematicBounds()) {
            double x = target.getLocation().getX();
            double y = target.getLocation().getY();
            double z = target.getLocation().getZ();
            inMine = x >= mine.getSchematicMinX() && x <= mine.getSchematicMaxX() && y >= mine.getSchematicMinY() && y <= mine.getSchematicMaxY() && z >= mine.getSchematicMinZ() && z <= mine.getSchematicMaxZ();
        }
        if (!inMine) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName());
            owner.sendMessage(configManager.getMessage("Messages.player-not-in-mine", replacements));
            return true;
        }
        World spawnWorld = Bukkit.getWorld("spawn");
        if (spawnWorld != null) {
            Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0);
            target.teleport(spawnLocation);
        } else {
            target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(configManager.getMessage("Messages.player-kicked", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(configManager.getMessage("Messages.you-were-kicked", targetReplacements));
        return true;
    }
} 