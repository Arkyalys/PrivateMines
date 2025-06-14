package fr.ju.privateMines.commands;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.managers.MineWorldManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineCommand implements CommandExecutor {
    private final PrivateMines plugin;
    private final MineManager mineManager;
    private final ConfigManager configManager;
    private final MineWorldManager mineWorldManager;
    private final Set<UUID> pendingResetConfirmation = new HashSet<>();
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private static class Console {
        public static final UUID UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
    public MineCommand(PrivateMines plugin) {
        this.plugin = plugin;
        this.mineManager = plugin.getMineManager();
        this.configManager = plugin.getConfigManager();
        this.mineWorldManager = plugin.getMineWorldManager();
        
        // Initialisation des sous-commandes
        subCommands.put("create", new MineCreateCommand(mineManager, configManager));
        subCommands.put("delete", new MineDeleteCommand(mineManager, configManager));
        subCommands.put("reset", new MineResetCommand(mineManager, configManager));
        subCommands.put("expand", new MineExpandCommand(mineManager, configManager));
        subCommands.put("settier", new MineSetTierCommand(mineManager, configManager));
        subCommands.put("tax", new MineTaxCommand(mineManager, configManager));
        subCommands.put("upgrade", new MineUpgradeCommand(mineManager, configManager));
        subCommands.put("teleport", new MineTeleportCommand(mineManager, configManager));
        subCommands.put("tp", new MineTeleportCommand(mineManager, configManager));
        subCommands.put("visit", new MineVisitCommand(mineManager, configManager));
        
        // Nouvelles sous-commandes
        subCommands.put("gui", new MineGuiCommand(mineManager, configManager, plugin));
        subCommands.put("menu", new MineGuiCommand(mineManager, configManager, plugin));
        subCommands.put("pregen", new MinePregenCommand(mineManager, configManager));
        subCommands.put("savestats", new MineSaveStatsCommand(mineManager, configManager, plugin));
        subCommands.put("stats", new MineStatsCommand(mineManager, configManager, plugin));
        subCommands.put("reload", new MineReloadCommand(mineManager, configManager, plugin));
        subCommands.put("stats-sync", new MineStatsSyncCommand(mineManager, configManager, plugin));
        subCommands.put("add", new MineAddCommand(mineManager, configManager, plugin));
        subCommands.put("remove", new MineRemoveCommand(mineManager, configManager, plugin));
        subCommands.put("debug", new MineDebugCommand(mineManager, configManager, plugin));
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return handleAdminCommands(sender, command, label, args, isPlayer);
        }
        
        if (!isPlayer) {
            sender.sendMessage(configManager.getMessage("mine-only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        if (subCommands.containsKey(subCommand)) {
            return subCommands.get(subCommand).execute(player, args, sender, command, label);
        } else {
            handleUnknownCommand(player);
            return true;
        }
    }
    private boolean handleAdminCommands(CommandSender sender, Command command, String label, String[] args, boolean isPlayer) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        if (isResetCommand(args)) {
            return handleResetCommand(sender, label, args, isPlayer);
        }
        if (isGuiCommand(args)) {
            return handleGuiCommandAdmin(sender);
        }
        sendAdminUsage(sender, label);
        return true;
    }
    private boolean isResetCommand(String[] args) {
        return args.length >= 2 && args[1].equalsIgnoreCase("reset");
    }
    private boolean isGuiCommand(String[] args) {
        return args.length >= 2 && args[1].equalsIgnoreCase("gui");
    }
    private boolean handleResetCommand(CommandSender sender, String label, String[] args, boolean isPlayer) {
        if (!sender.hasPermission(Permissions.ADMIN_FULL_RESET)) {
            sender.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        UUID senderId = isPlayer ? ((Player) sender).getUniqueId() : Console.UUID;
        if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
            if (pendingResetConfirmation.contains(senderId)) {
                pendingResetConfirmation.remove(senderId);
                sender.sendMessage(configManager.getMessage("mine-reset-in-progress"));
                performFullReset(sender, label);
                return true;
            } else {
                sender.sendMessage(configManager.getMessage("mine-reset-no-pending"));
                return true;
            }
        } else {
            sender.sendMessage(configManager.getMessage("mine-reset-warning").replace("%world%", mineWorldManager.getMineWorldName()));
            sender.sendMessage(configManager.getMessage("mine-reset-confirm").replace("%command%", label));
            pendingResetConfirmation.add(senderId);
            Bukkit.getScheduler().runTaskLater(plugin, () -> pendingResetConfirmation.remove(senderId), 30 * 20L);
            return true;
        }
    }
    private boolean handleGuiCommandAdmin(CommandSender sender) {
        if (plugin.getGUIManager() != null) {
            sender.sendMessage(configManager.getMessage("mine-gui-already-installed"));
            return true;
        }
        sender.sendMessage(configManager.getMessage("mine-gui-installing"));
        try {
            plugin.initializeGuiManager();
            sender.sendMessage(configManager.getMessage("mine-gui-installed"));
            sender.sendMessage(configManager.getMessage("mine-gui-usage"));
        } catch (Exception e) {
            sender.sendMessage(configManager.getMessage("mine-gui-install-error").replace("%error%", e.getMessage()));
            plugin.getLogger().severe("Error installing GUI system: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
    private void sendAdminUsage(CommandSender sender, String label) {
        sender.sendMessage(configManager.getMessage("mine-usage-admin-reset").replace("%command%", label));
        sender.sendMessage(configManager.getMessage("mine-usage-admin-gui").replace("%command%", label));
    }
    private void sendHelp(Player player) {
        fr.ju.privateMines.commands.utils.MineCommandUtils.sendHelp(plugin, mineManager, configManager, player);
    }
    private boolean performFullReset(CommandSender sender, String label) {
        try {
            sender.sendMessage(configManager.getMessage("mine-reset-step1"));
            List<UUID> mineOwners = new ArrayList<>(mineManager.mineMemoryService.getPlayerMines().keySet());
            for (UUID ownerId : mineOwners) {
                mineManager.removeMine(ownerId);
            }
            sender.sendMessage(configManager.getMessage("mine-reset-step2"));
            String worldName = mineWorldManager.getMineWorldName();
            File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                for (Player playerInWorld : world.getPlayers()) {
                    playerInWorld.sendMessage(configManager.getMessage("mine-world-reset"));
                    World spawnWorld = plugin.getServer().getWorlds().get(0);
                    playerInWorld.teleport(spawnWorld.getSpawnLocation());
                }
                boolean unloaded = plugin.getServer().unloadWorld(world, false);
                if (unloaded) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%world%", worldName);
                    sender.sendMessage(configManager.getMessage("mine-reset-world-unloaded", replacements));
                    try {
                        org.bukkit.util.FileUtil.copy(worldFolder, worldFolder);
                        sender.sendMessage(configManager.getMessage("mine-reset-world-deleted", replacements));
                    } catch (Exception e) {
                        replacements.put("%error%", e.getMessage());
                        sender.sendMessage(configManager.getMessage("mine-reset-world-delete-error", replacements));
                    }
                } else {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%world%", worldName);
                    sender.sendMessage(configManager.getMessage("mine-reset-world-unload-error", replacements));
                }
            } else {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("%world%", worldName);
                sender.sendMessage(configManager.getMessage("mine-reset-world-not-loaded", replacements));
            }
            sender.sendMessage(configManager.getMessage("mine-reset-step3"));
            File dataFile = new File(plugin.getDataFolder(), "data.yml");
            if (dataFile.exists() && dataFile.delete()) {
                sender.sendMessage(configManager.getMessage("mine-reset-data-deleted"));
            } else {
                sender.sendMessage(configManager.getMessage("mine-reset-data-error"));
            }
            File statsFile = new File(plugin.getDataFolder(), "stats.yml");
            if (statsFile.exists() && statsFile.delete()) {
                sender.sendMessage(configManager.getMessage("mine-reset-stats-deleted"));
            } else {
                sender.sendMessage(configManager.getMessage("mine-reset-stats-error"));
            }
            sender.sendMessage(configManager.getMessage("mine-reset-step4"));
            plugin.getCacheManager().clear();
            plugin.reloadPlugin();
            sender.sendMessage(configManager.getMessage("mine-reset-completed"));
            sender.sendMessage(configManager.getMessage("mine-reset-restart"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void handleUnknownCommand(Player player) {
        player.sendMessage(configManager.getMessage("mine-usage-unknown"));
    }
} 