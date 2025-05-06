package fr.ju.privateMines.commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.Permissions;

public class MineTabCompleter implements TabCompleter {
    private static final List<String> EMPTY_LIST = Collections.emptyList();
    private static final List<String> BASE_COMMANDS = Arrays.asList(
        "create", "delete", "reset", "expand", "settax", "teleport", "tp", "stats", 
        "upgrade", "pregen", "settier", "visit", "gui", "menu", "add", "remove", "debug"
    );
    
    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
        "stats-sync", "savestats", "reload"
    );
    
    private static final List<String> TIER_OPTIONS = Arrays.asList("1", "2", "3", "4", "5");
    
    // Map de gestionnaires de complétion pour les sous-commandes
    private final Map<String, BiFunction<Player, String[], List<String>>> subCommandCompleters = new HashMap<>();
    
    public MineTabCompleter(PrivateMines plugin) {
        initializeSubCommandCompleters();
    }
    
    private void initializeSubCommandCompleters() {
        // Ajout des gestionnaires pour chaque sous-commande
        subCommandCompleters.put("settier", this::getSettierTabCompletions);
        subCommandCompleters.put("stats", this::getStatsTabCompletions);
        subCommandCompleters.put("visit", this::getVisitTabCompletions);
        subCommandCompleters.put("add", this::getPlayerListCompletions);
        subCommandCompleters.put("remove", this::getPlayerListCompletions);
        subCommandCompleters.put("stats-sync", this::getPlayerListCompletions);
        subCommandCompleters.put("debug", this::getDebugTabCompletions);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return EMPTY_LIST;
        }
        
        Player player = (Player) sender;
        
        // Premier argument : proposer les commandes de base + commandes admin si permission
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(BASE_COMMANDS);
            
            if (player.hasPermission("privateMines.admin")) {
                suggestions.addAll(ADMIN_COMMANDS);
            }
            
            return filterStartingWith(suggestions, args[0]);
        }
        
        // Arguments suivants : utiliser le gestionnaire spécifique à la sous-commande
        String subCommand = args[0].toLowerCase();
        BiFunction<Player, String[], List<String>> completer = subCommandCompleters.get(subCommand);
        
        if (completer != null) {
            return completer.apply(player, args);
        }
        
        return EMPTY_LIST;
    }
    
    private List<String> getSettierTabCompletions(Player player, String[] args) {
        if (!player.hasPermission(Permissions.ADMIN_SET_TIER)) {
            return EMPTY_LIST;
        }
        
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        } else if (args.length == 3) {
            return filterStartingWith(TIER_OPTIONS, args[2]);
        }
        
        return EMPTY_LIST;
    }
    
    private List<String> getStatsTabCompletions(Player player, String[] args) {
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>(getOnlinePlayerNames(args[1]));
            if ("top".startsWith(args[1].toLowerCase())) {
                suggestions.add("top");
            }
            return suggestions;
        }
        
        return EMPTY_LIST;
    }
    
    private List<String> getVisitTabCompletions(Player player, String[] args) {
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        
        return EMPTY_LIST;
    }
    
    private List<String> getPlayerListCompletions(Player player, String[] args) {
        if (args.length == 2) {
            return getOnlinePlayerNames(args[1]);
        }
        
        return EMPTY_LIST;
    }
    
    private List<String> getDebugTabCompletions(Player player, String[] args) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            return EMPTY_LIST;
        }
        
        if (args.length == 2) {
            return filterStartingWith(Arrays.asList("on", "off"), args[1]);
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