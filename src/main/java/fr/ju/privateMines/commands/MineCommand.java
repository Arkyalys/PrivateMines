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
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
    private Map.Entry<UUID, Long> pendingReset = null;
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
        subCommands.put("kick", new MineKickCommand(mineManager, configManager));
        subCommands.put("ban", new MineBanCommand(mineManager, configManager));
        subCommands.put("unban", new MineUnbanCommand(mineManager, configManager));
        subCommands.put("deny", new MineDenyCommand(mineManager, configManager));
        subCommands.put("allow", new MineAllowCommand(mineManager, configManager));
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission(Permissions.ADMIN)) {
                 sender.sendMessage(configManager.getMessage("Messages.no-permission"));
                 return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                 if (!sender.hasPermission(Permissions.ADMIN_FULL_RESET)) {
                     sender.sendMessage(configManager.getMessage("Messages.no-permission"));
                     return true;
                 }
                 UUID senderId = isPlayer ? ((Player) sender).getUniqueId() : Console.UUID;
                 if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
                     if (pendingResetConfirmation.contains(senderId)) {
                         pendingResetConfirmation.remove(senderId);
                         sender.sendMessage(ColorUtil.deserialize("&cRéinitialisation complète en cours... NE PAS REDÉMARRER LE SERVEUR."));
                         performFullReset(sender, label);
                         return true;
                     } else {
                         sender.sendMessage(ColorUtil.deserialize("&cVous n'avez pas de réinitialisation en attente de confirmation ou elle a expiré."));
                         return true;
                     }
                 } else {
                     sender.sendMessage(ColorUtil.deserialize("&c&lATTENTION ! &cCette commande va supprimer TOUTES les mines,"));
                     sender.sendMessage(ColorUtil.deserialize("&cleurs régions WorldGuard associées (si applicable),"));
                     sender.sendMessage(ColorUtil.deserialize("&cle monde des mines dédié (" + mineWorldManager.getMineWorldName() + "),"));
                     sender.sendMessage(ColorUtil.deserialize("&cet les fichiers de données du plugin (`data.yml`, `stats.yml`)."));
                     sender.sendMessage(ColorUtil.deserialize("&cCette action est IRRÉVERSIBLE."));
                     sender.sendMessage(ColorUtil.deserialize("&ePour confirmer, tapez &f/" + label + " admin reset confirm &edans les 30 secondes."));
                     pendingResetConfirmation.add(senderId);
                     Bukkit.getScheduler().runTaskLater(plugin, () -> pendingResetConfirmation.remove(senderId), 30 * 20L);
                     return true;
                 }
            }
            else if (args.length >= 2 && args[1].equalsIgnoreCase("gui")) {
                sender.sendMessage(ColorUtil.deserialize("&aInstalling GUI system..."));
                try {
                    if (plugin.getGUIManager() == null) {
                        java.lang.reflect.Field field = PrivateMines.class.getDeclaredField("guiManager");
                        field.setAccessible(true);
                        field.set(plugin, new fr.ju.privateMines.utils.GUIManager(plugin));
                        sender.sendMessage(ColorUtil.deserialize("&aGUI Manager initialized."));
                    }
                    plugin.getServer().getPluginManager().registerEvents(new fr.ju.privateMines.listeners.GUIListener(plugin), plugin);
                    sender.sendMessage(ColorUtil.deserialize("&aGUI system installed successfully!"));
                    sender.sendMessage(ColorUtil.deserialize("&aPlayers can now use &f/jumine gui &aor &f/jumine menu &ato access the interface."));
                } catch (Exception e) {
                    sender.sendMessage(ColorUtil.deserialize("&cError installing GUI system: " + e.getMessage()));
                    plugin.getLogger().severe("Error installing GUI system: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
            else {
                 sender.sendMessage(ColorUtil.deserialize("&cUsage:"));
                 sender.sendMessage(ColorUtil.deserialize("&c/" + label + " admin reset - Reset all mines"));
                 sender.sendMessage(ColorUtil.deserialize("&c/" + label + " admin gui - Install GUI system"));
                 return true;
            }
        }
        if (!isPlayer) {
            sender.sendMessage(configManager.getMessage("Messages.only-players"));
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
            case "kick":
            case "ban":
            case "unban":
            case "deny":
            case "allow":
                return subCommands.get(args[0].toLowerCase()).execute(player, args, sender, command, label);
            case "gui":
            case "menu":
                MineMainGUI.openGUI(player);
                break;
            case "pregen":
                if (!player.hasPermission(Permissions.ADMIN_PREGEN)) {
                    player.sendMessage(configManager.getMessage("Messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.translateColors("&cUsage: /mine pregen <number> [type]"));
                    return true;
                }
                try {
                    int count = Integer.parseInt(args[1]);
                    String type = args.length > 2 ? args[2] : "default";
                    mineManager.pregenMines(player, count, type);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.translateColors("&cThe number must be a valid integer."));
                }
                break;
            case "savestats":
                if (!player.hasPermission(Permissions.ADMIN_RELOAD)) {
                    player.sendMessage(configManager.getMessage("Messages.no-permission"));
                    return true;
                }
                player.sendMessage(ColorUtil.deserialize("&6Saving statistics..."));
                if (plugin.getStatsManager() != null) {
                    plugin.getStatsManager().saveStats();
                    player.sendMessage(ColorUtil.deserialize("&aStatistics have been successfully saved!"));
                } else {
                    player.sendMessage(ColorUtil.deserialize("&cThe stats manager is not enabled."));
                }
                break;
            case "stats":
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("top")) {
                        showTopStats(player);
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(configManager.getMessage("Messages.invalid-player"));
                        return true;
                    }
                    showPlayerStats(player, target.getUniqueId());
                } else {
                    showPlayerStats(player, player.getUniqueId());
                }
                break;
            case "reload":
                if (!player.hasPermission(Permissions.ADMIN_RELOAD)) {
                    player.sendMessage(configManager.getMessage("Messages.no-permission"));
                    return true;
                }
                player.sendMessage(ColorUtil.translateColors("&6Reloading plugin..."));
                if (plugin.reloadPlugin()) {
                    player.sendMessage(ColorUtil.translateColors("&aThe plugin has been successfully reloaded!"));
                } else {
                    player.sendMessage(ColorUtil.translateColors("&cError while reloading the plugin, check the console for more details."));
                }
                break;
            case "stats-sync":
                if (!player.hasPermission("privateMines.admin")) {
                    player.sendMessage(configManager.getMessage("Messages.no-permission"));
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
            case "add":
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.translateColors("&cUtilisation: /mine add <joueur>"));
                    return true;
                }
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ColorUtil.translateColors("&cJoueur introuvable."));
                    return true;
                }
                if (!mineManager.hasMine(player)) {
                    player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine."));
                    return true;
                }
                mineManager.getMineProtectionManager().addMemberToMineRegion(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(ColorUtil.translateColors("&a" + target.getName() + " est maintenant contributeur de votre mine !"));
                return true;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }
    private void showPlayerStats(Player viewer, UUID ownerUUID) {
        MineCommandUtils.showPlayerStats(plugin, mineManager, configManager, viewer, ownerUUID);
    }
    private void showTopStats(Player viewer) {
        MineCommandUtils.showTopStats(plugin, mineManager, viewer);
    }
    private String formatTimestamp(long timestamp) {
        return MineCommandUtils.formatTimestamp(timestamp);
    }
    private void sendHelp(Player player) {
        MineCommandUtils.sendHelp(plugin, mineManager, configManager, player);
    }
    private boolean performFullReset(CommandSender sender, String label) {
        try {
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-step1"));
            List<UUID> mineOwners = new ArrayList<>(mineManager.mineMemoryService.getPlayerMines().keySet());
            int mineCount = mineOwners.size();
            for (UUID ownerId : mineOwners) {
                mineManager.removeMine(ownerId);
            }
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-step2"));
            String worldName = mineWorldManager.getMineWorldName();
            File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                for (Player playerInWorld : world.getPlayers()) {
                    playerInWorld.sendMessage(configManager.getMessage("Messages.mine-world-reset"));
                    World spawnWorld = plugin.getServer().getWorlds().get(0);
                    playerInWorld.teleport(spawnWorld.getSpawnLocation());
                }
                boolean unloaded = plugin.getServer().unloadWorld(world, false);
                if (unloaded) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%world%", worldName);
                    sender.sendMessage(configManager.getMessage("Messages.admin-reset-world-unloaded", replacements));
                    try {
                        FileUtils.deleteDirectory(worldFolder);
                        sender.sendMessage(configManager.getMessage("Messages.admin-reset-world-deleted", replacements));
                    } catch (IOException e) {
                        replacements.put("%error%", e.getMessage());
                        sender.sendMessage(configManager.getMessage("Messages.admin-reset-world-delete-error", replacements));
                    }
                } else {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%world%", worldName);
                    sender.sendMessage(configManager.getMessage("Messages.admin-reset-world-unload-error", replacements));
                }
            } else {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("%world%", worldName);
                sender.sendMessage(configManager.getMessage("Messages.admin-reset-world-not-loaded", replacements));
            }
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-step3"));
            File dataFile = new File(plugin.getDataFolder(), "data.yml");
            if (dataFile.exists() && dataFile.delete()) {
                sender.sendMessage(configManager.getMessage("Messages.admin-reset-data-deleted"));
            } else {
                sender.sendMessage(configManager.getMessage("Messages.admin-reset-data-error"));
            }
            File statsFile = new File(plugin.getDataFolder(), "stats.yml");
            if (statsFile.exists() && statsFile.delete()) {
                sender.sendMessage(configManager.getMessage("Messages.admin-reset-stats-deleted"));
            } else {
                sender.sendMessage(configManager.getMessage("Messages.admin-reset-stats-error"));
            }
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-step4"));
            plugin.getCacheManager().clear();
            plugin.reloadPlugin();
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-completed"));
            sender.sendMessage(configManager.getMessage("Messages.admin-reset-restart"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private void kickPlayerFromMine(Player owner, Player target) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(configManager.getMessage("Messages.cannot-kick-self"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        World mineWorld = mine.getLocation().getWorld();
        if (!target.getWorld().equals(mineWorld)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName());
            owner.sendMessage(configManager.getMessage("Messages.player-not-in-mine", replacements));
            return;
        }
        if (mine.hasMineArea()) {
            int targetX = target.getLocation().getBlockX();
            int targetY = target.getLocation().getBlockY();
            int targetZ = target.getLocation().getBlockZ();
            if (targetX < mine.getMinX() || targetX > mine.getMaxX() ||
                targetY < mine.getMinY() || targetY > mine.getMaxY() ||
                targetZ < mine.getMinZ() || targetZ > mine.getMaxZ()) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("%player%", target.getName());
                owner.sendMessage(configManager.getMessage("Messages.player-not-in-mine", replacements));
                return;
            }
        } else if (mine.hasSchematicBounds()) {
            double targetX = target.getLocation().getX();
            double targetY = target.getLocation().getY();
            double targetZ = target.getLocation().getZ();
            if (targetX < mine.getSchematicMinX() || targetX > mine.getSchematicMaxX() ||
                targetY < mine.getSchematicMinY() || targetY > mine.getSchematicMaxY() ||
                targetZ < mine.getSchematicMinZ() || targetZ > mine.getSchematicMaxZ()) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("%player%", target.getName());
                owner.sendMessage(configManager.getMessage("Messages.player-not-in-mine", replacements));
                return;
            }
        }
        World spawnWorld = Bukkit.getWorld("spawn");
        if (spawnWorld != null) {
            Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0); 
            target.teleport(spawnLocation);
            Map<String, String> ownerReplacements = new HashMap<>();
            ownerReplacements.put("%player%", target.getName());
            owner.sendMessage(configManager.getMessage("Messages.player-kicked", ownerReplacements));
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("%owner%", owner.getName());
            target.sendMessage(configManager.getMessage("Messages.you-were-kicked", targetReplacements));
        } else {
            target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            Map<String, String> ownerReplacements = new HashMap<>();
            ownerReplacements.put("%player%", target.getName());
            owner.sendMessage(configManager.getMessage("Messages.player-kicked", ownerReplacements));
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("%owner%", owner.getName());
            target.sendMessage(configManager.getMessage("Messages.you-were-kicked", targetReplacements));
            plugin.getLogger().warning("Le monde 'spawn' n'a pas été trouvé. Le joueur " + target.getName() + 
                " a été téléporté au spawn du monde par défaut.");
        }
    }
    private void banPlayerFromMine(Player owner, Player target, long duration) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(configManager.getMessage("Messages.cannot-ban-self"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        mine.banPlayer(target.getUniqueId(), duration);
        mineManager.saveMine(mine);
        if (target.getWorld().equals(mine.getLocation().getWorld())) {
            boolean inMine = false;
            if (mine.hasMineArea()) {
                int targetX = target.getLocation().getBlockX();
                int targetY = target.getLocation().getBlockY();
                int targetZ = target.getLocation().getBlockZ();
                if (targetX >= mine.getMinX() && targetX <= mine.getMaxX() &&
                    targetY >= mine.getMinY() && targetY <= mine.getMaxY() &&
                    targetZ >= mine.getMinZ() && targetZ <= mine.getMaxZ()) {
                    inMine = true;
                }
            } else if (mine.hasSchematicBounds()) {
                double targetX = target.getLocation().getX();
                double targetY = target.getLocation().getY();
                double targetZ = target.getLocation().getZ();
                if (targetX >= mine.getSchematicMinX() && targetX <= mine.getSchematicMaxX() &&
                    targetY >= mine.getSchematicMinY() && targetY <= mine.getSchematicMaxY() &&
                    targetZ >= mine.getSchematicMinZ() && targetZ <= mine.getSchematicMaxZ()) {
                    inMine = true;
                }
            }
            if (inMine) {
                World spawnWorld = Bukkit.getWorld("spawn");
                if (spawnWorld != null) {
                    Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0); 
                    target.teleport(spawnLocation);
                } else {
                    target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }
        String formattedDuration = formatDuration(duration);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        ownerReplacements.put("%duration%", formattedDuration);
        owner.sendMessage(configManager.getMessage("Messages.player-banned", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        targetReplacements.put("%duration%", formattedDuration);
        target.sendMessage(configManager.getMessage("Messages.you-were-banned", targetReplacements));
    }
    private void banPlayerPermanently(Player owner, Player target) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(configManager.getMessage("Messages.cannot-ban-self"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        mine.banPlayerPermanently(target.getUniqueId());
        mineManager.saveMine(mine);
        if (target.getWorld().equals(mine.getLocation().getWorld())) {
            boolean inMine = false;
            if (mine.hasMineArea()) {
                int targetX = target.getLocation().getBlockX();
                int targetY = target.getLocation().getBlockY();
                int targetZ = target.getLocation().getBlockZ();
                if (targetX >= mine.getMinX() && targetX <= mine.getMaxX() &&
                    targetY >= mine.getMinY() && targetY <= mine.getMaxY() &&
                    targetZ >= mine.getMinZ() && targetZ <= mine.getMaxZ()) {
                    inMine = true;
                }
            } else if (mine.hasSchematicBounds()) {
                double targetX = target.getLocation().getX();
                double targetY = target.getLocation().getY();
                double targetZ = target.getLocation().getZ();
                if (targetX >= mine.getSchematicMinX() && targetX <= mine.getSchematicMaxX() &&
                    targetY >= mine.getSchematicMinY() && targetY <= mine.getSchematicMaxY() &&
                    targetZ >= mine.getSchematicMinZ() && targetZ <= mine.getSchematicMaxZ()) {
                    inMine = true;
                }
            }
            if (inMine) {
                World spawnWorld = Bukkit.getWorld("spawn");
                if (spawnWorld != null) {
                    Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0); 
                    target.teleport(spawnLocation);
                } else {
                    target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(configManager.getMessage("Messages.player-banned-permanently", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(configManager.getMessage("Messages.you-were-banned-permanently", targetReplacements));
    }
    private void unbanPlayerFromMine(Player owner, OfflinePlayer target) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (!mine.getMineAccess().isBanned(target.getUniqueId())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
            owner.sendMessage(configManager.getMessage("Messages.player-not-banned", replacements));
            return;
        }
        mine.unbanPlayer(target.getUniqueId());
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
        owner.sendMessage(configManager.getMessage("Messages.player-unbanned", ownerReplacements));
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("%owner%", owner.getName());
            onlineTarget.sendMessage(configManager.getMessage("Messages.you-were-unbanned", targetReplacements));
        }
    }
    private void denyPlayerAccess(Player owner, Player target) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(configManager.getMessage("Messages.cannot-deny-self"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        mine.denyAccess(target.getUniqueId());
        mineManager.saveMine(mine);
        if (target.getWorld().equals(mine.getLocation().getWorld())) {
            boolean inMine = false;
            if (mine.hasMineArea()) {
                int targetX = target.getLocation().getBlockX();
                int targetY = target.getLocation().getBlockY();
                int targetZ = target.getLocation().getBlockZ();
                if (targetX >= mine.getMinX() && targetX <= mine.getMaxX() &&
                    targetY >= mine.getMinY() && targetY <= mine.getMaxY() &&
                    targetZ >= mine.getMinZ() && targetZ <= mine.getMaxZ()) {
                    inMine = true;
                }
            } else if (mine.hasSchematicBounds()) {
                double targetX = target.getLocation().getX();
                double targetY = target.getLocation().getY();
                double targetZ = target.getLocation().getZ();
                if (targetX >= mine.getSchematicMinX() && targetX <= mine.getSchematicMaxX() &&
                    targetY >= mine.getSchematicMinY() && targetY <= mine.getSchematicMaxY() &&
                    targetZ >= mine.getSchematicMinZ() && targetZ <= mine.getSchematicMaxZ()) {
                    inMine = true;
                }
            }
            if (inMine) {
                World spawnWorld = Bukkit.getWorld("spawn");
                if (spawnWorld != null) {
                    Location spawnLocation = new Location(spawnWorld, 540.5, 120.0, 5.5, 180, 0); 
                    target.teleport(spawnLocation);
                } else {
                    target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName());
        owner.sendMessage(configManager.getMessage("Messages.player-denied", ownerReplacements));
        Map<String, String> targetReplacements = new HashMap<>();
        targetReplacements.put("%owner%", owner.getName());
        target.sendMessage(configManager.getMessage("Messages.you-were-denied", targetReplacements));
    }
    private void allowPlayerAccess(Player owner, OfflinePlayer target) {
        if (!mineManager.hasMine(owner)) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        Mine mine = mineManager.getMine(owner).orElse(null);
        if (mine == null) {
            owner.sendMessage(configManager.getMessage("Messages.no-mine"));
            return;
        }
        if (!mine.getMineAccess().isDenied(target.getUniqueId())) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
            owner.sendMessage(configManager.getMessage("Messages.player-not-denied", replacements));
            return;
        }
        mine.allowAccess(target.getUniqueId());
        mineManager.saveMine(mine);
        Map<String, String> ownerReplacements = new HashMap<>();
        ownerReplacements.put("%player%", target.getName() != null ? target.getName() : target.getUniqueId().toString());
        owner.sendMessage(configManager.getMessage("Messages.player-allowed", ownerReplacements));
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            Map<String, String> targetReplacements = new HashMap<>();
            targetReplacements.put("%owner%", owner.getName());
            onlineTarget.sendMessage(configManager.getMessage("Messages.you-were-allowed", targetReplacements));
        }
    }
    private String formatDuration(long seconds) {
        return MineCommandUtils.formatDuration(seconds);
    }
    private long parseDuration(String durationStr) {
        return MineCommandUtils.parseDuration(durationStr);
    }
} 