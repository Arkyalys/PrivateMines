package fr.ju.privateMines.commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineDeleteCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineDeleteCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.DELETE)) {
            player.sendMessage(configManager.getMessage("Messages.no-permission"));
            return true;
        }
        mineManager.deleteMine(player);
        org.bukkit.World spawnWorld = Bukkit.getWorld("spawn");
        if (spawnWorld != null) {
            Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0);
            player.teleport(spawnLocation);
        } else {
            player.sendMessage(configManager.getMessage("Messages.spawn-world-not-found"));
        }
        return true;
    }
} 