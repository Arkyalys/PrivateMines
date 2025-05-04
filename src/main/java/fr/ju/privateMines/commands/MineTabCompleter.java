package fr.ju.privateMines.commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.Permissions;
public class MineTabCompleter implements TabCompleter {
    private final List<String> EMPTY_LIST = new ArrayList<>();
    private final List<String> BASE_COMMANDS = Arrays.asList(
        "create", "delete", "reset", "expand", "settype", "settax", "teleport", "tp", "stats", "upgrade", "pregen", "settier", "visit", "gui", "menu"
    );
    public MineTabCompleter(PrivateMines plugin) {
        // Constructeur vide
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return EMPTY_LIST;
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            return filterStartingWith(BASE_COMMANDS, args[0]);
        }
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "settier":
                    if (!player.hasPermission(Permissions.ADMIN_SET_TIER)) {
                        return EMPTY_LIST;
                    }
                    if (args.length == 2) {
                        return getOnlinePlayerNames(args[1]);
                    } else if (args.length == 3) {
                        return Arrays.asList("1", "2", "3");
                    }
                    break;
                case "stats":
                    if (args.length == 2) {
                        List<String> suggestions = new ArrayList<>(getOnlinePlayerNames(args[1]));
                        if ("top".startsWith(args[1].toLowerCase())) {
                            suggestions.add("top");
                        }
                        return suggestions;
                    }
                    break;
                case "visit":
                    if (args.length == 2) {
                        return getOnlinePlayerNames(args[1]);
                    }
                    break;
            }
        }
        if (player.hasPermission("privateMines.admin")) {
            EMPTY_LIST.add("stats-sync");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats-sync") && player.hasPermission("privateMines.admin")) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    EMPTY_LIST.add(onlinePlayer.getName());
                }
            }
        }
        return EMPTY_LIST;
    }
    private List<String> filterStartingWith(List<String> options, String prefix) {
        if (prefix.isEmpty()) {
            return options;
        }
        String lowercasePrefix = prefix.toLowerCase();
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(lowercasePrefix))
            .collect(Collectors.toList());
    }
    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
} 