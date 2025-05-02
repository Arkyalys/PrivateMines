package fr.ju.privateMines.commands;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.commands.utils.MineCommandUtils;
import fr.ju.privateMines.guis.MineMainGUI;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.managers.MineWorldManager;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
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
        subCommands.put("create", new MineCreateCommand(mineManager, configManager));
        subCommands.put("delete", new MineDeleteCommand(mineManager, configManager));
        subCommands.put("reset", new MineResetCommand(mineManager, configManager));
        subCommands.put("expand", new MineExpandCommand(mineManager, configManager));
        subCommands.put("settype", new MineSetTypeCommand(mineManager, configManager));
        subCommands.put("settier", new MineSetTierCommand(mineManager, configManager));
        subCommands.put("tax", new MineTaxCommand(mineManager, configManager));
        subCommands.put("upgrade", new MineUpgradeCommand(mineManager, configManager));
        subCommands.put("teleport", new MineTeleportCommand(mineManager, configManager));
        subCommands.put("tp", new MineTeleportCommand(mineManager, configManager));
        subCommands.put("visit", new MineVisitCommand(mineManager, configManager));
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
        switch (args[0].toLowerCase()) {
            case "create":
            case "delete":
            case "reset":
            case "expand":
            case "settype":
            case "settier":
            case "tax":
            case "upgrade":
            case "teleport":
            case "tp":
            case "visit":
                return subCommands.get(args[0].toLowerCase()).execute(player, args, sender, command, label);
            case "gui":
            case "menu":
                handleGuiCommand(player);
                break;
            case "pregen":
                return handlePregenCommand(player, args);
            case "savestats":
                return handleSaveStatsCommand(player);
            case "stats":
                return handleStatsCommand(player, args);
            case "reload":
                return handleReloadCommand(player);
            case "stats-sync":
                return handleStatsSyncCommand(player, args);
            case "add":
                return handleAddCommand(player, args);
            case "remove":
                return handleRemoveCommand(player, args);
            case "debug":
                return handleDebugCommand(player, args);
            default:
                handleUnknownCommand(player);
                break;
        }
        return true;
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
            // TODO: Remplacer la réflexion par une méthode d'initialisation propre dans PrivateMines
            java.lang.reflect.Field field = PrivateMines.class.getDeclaredField("guiManager");
            // field.setAccessible(true); // Suppression de setAccessible
            field.set(plugin, new fr.ju.privateMines.utils.GUIManager(plugin));
            sender.sendMessage(configManager.getMessage("mine-gui-installed"));
            plugin.getServer().getPluginManager().registerEvents(new fr.ju.privateMines.listeners.GUIListener(plugin), plugin);
            sender.sendMessage(configManager.getMessage("mine-gui-system-installed"));
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
    private void handleGuiCommand(Player player) {
        MineMainGUI.openGUI(player);
    }
    private boolean handlePregenCommand(Player player, String[] args) {
        if (!player.hasPermission(Permissions.ADMIN_PREGEN)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("mine-usage-pregen"));
            return true;
        }
        try {
            int count = Integer.parseInt(args[1]);
            String type = args.length > 2 ? args[2] : "default";
            mineManager.pregenMines(player, count, type);
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getMessage("mine-invalid-number"));
        }
        return true;
    }
    private boolean handleSaveStatsCommand(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_RELOAD)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        player.sendMessage(configManager.getMessage("mine-stats-saving"));
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().saveStats();
            player.sendMessage(configManager.getMessage("mine-stats-saved"));
        } else {
            player.sendMessage(configManager.getMessage("mine-stats-not-enabled"));
        }
        return true;
    }
    private boolean handleStatsCommand(Player player, String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("top")) {
                showTopStats(player);
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(configManager.getMessage("mine-invalid-player"));
                return true;
            }
            showPlayerStats(player, target.getUniqueId());
        } else {
            showPlayerStats(player, player.getUniqueId());
        }
        return true;
    }
    private boolean handleReloadCommand(Player player) {
        if (!player.hasPermission(Permissions.ADMIN_RELOAD)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        player.sendMessage(configManager.getMessage("mine-reload-start"));
        if (plugin.reloadPlugin()) {
            player.sendMessage(configManager.getMessage("mine-reload-success"));
        } else {
            player.sendMessage(configManager.getMessage("mine-reload-failed"));
        }
        return true;
    }
    private boolean handleStatsSyncCommand(Player player, String[] args) {
        if (!player.hasPermission("privateMines.admin")) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        if (args.length > 1) {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cJoueur non trouvé: " + args[1]);
                return true;
            }
            if (!mineManager.hasMine(target)) {
                player.sendMessage("§cCe joueur n'a pas de mine.");
                return true;
            }
            Mine targetMine = mineManager.getMine(target).orElse(null);
            if (targetMine != null) {
                if (targetMine.hasMineArea()) {
                    targetMine.calculateTotalBlocks();
                }
                targetMine.synchronizeStats();
                player.sendMessage("§aStatistiques synchronisées pour la mine de " + target.getName() + ".");
                player.sendMessage("§fBlocs: " + targetMine.getStats().getBlocksMined() + "/" + 
                                  targetMine.getStats().getTotalBlocks() + " (" + 
                                  targetMine.getStats().getPercentageMined() + "%)");
            }
        }
        else {
            int count = 0;
            for (Mine currentMine : mineManager.getAllMines()) {
                if (currentMine.hasMineArea()) {
                    currentMine.calculateTotalBlocks();
                    currentMine.synchronizeStats();
                    count++;
                }
            }
            player.sendMessage("§aStatistiques synchronisées pour " + count + " mines.");
            plugin.getStatsManager().saveStats();
            player.sendMessage("§aStatistiques sauvegardées.");
        }
        return true;
    }
    private boolean handleAddCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translateColors("&cUsage: /mine add <player>"));
            return true;
        }
        Player addTarget = player.getServer().getPlayer(args[1]);
        if (addTarget == null) {
            player.sendMessage(ColorUtil.translateColors("&cPlayer not found."));
            return true;
        }
        if (!mineManager.hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cYou do not own a mine."));
            return true;
        }
        mineManager.getMineProtectionManager().addMemberToMineRegion(player.getUniqueId(), addTarget.getUniqueId());
        player.sendMessage(ColorUtil.translateColors("&a" + addTarget.getName() + " has been added to your mine!"));
        return true;
    }
    private boolean handleRemoveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translateColors("&cUsage: /mine remove <player>"));
            return true;
        }
        Player removeTarget = player.getServer().getPlayer(args[1]);
        if (removeTarget == null) {
            player.sendMessage(ColorUtil.translateColors("&cPlayer not found."));
            return true;
        }
        if (!mineManager.hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cYou do not own a mine."));
            return true;
        }
        mineManager.getMineProtectionManager().removeMemberFromMineRegion(player.getUniqueId(), removeTarget.getUniqueId());
        player.sendMessage(ColorUtil.translateColors("&a" + removeTarget.getName() + " has been removed from your mine!"));
        return true;
    }
    private boolean handleDebugCommand(Player player, String[] args) {
        if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            player.sendMessage("§eUsage: /mine debug on|off");
            return true;
        }
        boolean enable = args[1].equalsIgnoreCase("on");
        fr.ju.privateMines.PrivateMines.setDebugMode(enable);
        player.sendMessage(enable ? "§aDebug mode enabled." : "§cDebug mode disabled.");
        return true;
    }
    private void handleUnknownCommand(Player player) {
        player.sendMessage(configManager.getMessage("mine-usage-unknown"));
    }
    private void showPlayerStats(Player viewer, UUID ownerUUID) {
        MineCommandUtils.showPlayerStats(plugin, mineManager, configManager, viewer, ownerUUID);
    }
    private void showTopStats(Player viewer) {
        MineCommandUtils.showTopStats(plugin, mineManager, viewer);
    }
    private void sendHelp(Player player) {
        MineCommandUtils.sendHelp(plugin, mineManager, configManager, player);
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
                        FileUtils.deleteDirectory(worldFolder);
                        sender.sendMessage(configManager.getMessage("mine-reset-world-deleted", replacements));
                    } catch (IOException e) {
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
} 