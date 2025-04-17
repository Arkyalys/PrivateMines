package fr.ju.privateMines.commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
public interface SubCommand {
    boolean execute(Player player, String[] args, CommandSender sender, Command command, String label);
} 