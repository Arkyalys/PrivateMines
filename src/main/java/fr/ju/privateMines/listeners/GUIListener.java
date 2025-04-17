package fr.ju.privateMines.listeners;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.guis.MineCompositionGUI;
import fr.ju.privateMines.guis.MineExpandGUI;
import fr.ju.privateMines.guis.MineMainGUI;
import fr.ju.privateMines.guis.MineSettingsGUI;
import fr.ju.privateMines.guis.MineStatsGUI;
import fr.ju.privateMines.guis.MineTypeGUI;
import fr.ju.privateMines.guis.MineVisitorsGUI;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ColorUtil;
public class GUIListener implements Listener {
    private final PrivateMines plugin;
    public GUIListener(PrivateMines plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        Player player = (Player) event.getWhoClicked();
        String inventoryType = plugin.getGUIManager().getOpenInventoryType(player);
        if (inventoryType == null) return; 
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        switch (inventoryType.split(":")[0]) {
            case "mine_main":
                handleMainGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_stats":
                handleStatsGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_visitors":
                handleVisitorsGUIClick(player, clickedItem, event.getSlot(), inventoryType, event);
                break;
            case "mine_visitor_action":
                handleVisitorActionGUIClick(player, clickedItem, event.getSlot(), inventoryType);
                break;
            case "mine_settings":
                handleSettingsGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_type":
                handleTypeGUIClick(player, clickedItem, event.getSlot(), inventoryType, event.getClickedInventory());
                break;
            case "mine_expand":
                handleExpandGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_composition":
                handleCompositionGUIClick(player, clickedItem, event.getSlot());
                break;
        }
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        plugin.getGUIManager().unregisterOpenInventory(player);
    }
    private void handleMainGUIClick(Player player, ItemStack clickedItem, int slot) {
        boolean hasMine = plugin.getMineManager().hasMine(player);
        if (hasMine) {
            switch (slot) {
                case 4: 
                    MineStatsGUI.openGUI(player);
                    break;
                case 20: 
                    player.closeInventory();
                    plugin.getMineManager().teleportToMine(player, player);
                    player.sendMessage(ColorUtil.translateColors("&aTéléportation à votre mine..."));
                    break;
                case 21: 
                    player.closeInventory();
                    plugin.getMineManager().resetMine(player);
                    break;
                case 22: 
                    toggleMineAccess(player);
                    MineMainGUI.openGUI(player);
                    break;
                case 23: 
                    MineSettingsGUI.openGUI(player);
                    break;
                case 24: 
                    MineVisitorsGUI.openGUI(player, 0);
                    break;
                case 30: 
                    MineExpandGUI.openGUI(player);
                    break;
                case 31: 
                    boolean upgradeResult = plugin.getMineManager().upgradeMine(player);
                    if (upgradeResult) {
                        player.closeInventory();
                    } else {
                        player.closeInventory();
                        MineMainGUI.openGUI(player); 
                    }
                    break;
                case 32: 
                    player.closeInventory();
                    player.sendMessage(ColorUtil.translateColors("&c⚠ Pour confirmer la suppression de votre mine, tapez &e/jumine delete&c."));
                    break;
            }
        } else {
            switch (slot) {
                case 22: 
                    player.closeInventory();
                    plugin.getMineManager().createMine(player, "default");
                    break;
                case 31: 
                    if (player.hasPermission("privateMines.admin.create")) {
                        MineTypeGUI.openGUI(player, false);
                    }
                    break;
            }
        }
    }
    private void handleStatsGUIClick(Player player, ItemStack clickedItem, int slot) {
        switch (slot) {
            case 16: 
                MineVisitorsGUI.openGUI(player, 0);
                break;
            case 22: 
                MineCompositionGUI.openGUI(player);
                break;
            case 31: 
                MineMainGUI.openGUI(player);
                break;
        }
    }
    private void handleVisitorsGUIClick(Player player, ItemStack clickedItem, int slot, String inventoryType, InventoryClickEvent event) {
        int currentPage = 0;
        String[] parts = inventoryType.split(":");
        if (parts.length > 1) {
            try {
                currentPage = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
            }
        }
        switch (slot) {
            case 45: 
                if (currentPage > 0) {
                    MineVisitorsGUI.openGUI(player, currentPage - 1);
                }
                return;
            case 53: 
                MineVisitorsGUI.openGUI(player, currentPage + 1);
                return;
            case 49: 
                MineMainGUI.openGUI(player);
                return;
        }
        if (slot >= 9 && slot <= 44) {
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine == null) {
                player.closeInventory();
                return;
            }
            MineStats stats = mine.getStats();
            Map<UUID, Integer> visitorStats = stats.getVisitorStats();
            List<Map.Entry<UUID, Integer>> sortedVisitors = visitorStats.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .filter(entry -> !entry.getKey().equals(mine.getOwner())) 
                    .collect(Collectors.toList());
            int startIndex = currentPage * MineVisitorsGUI.PAGE_SIZE;
            int visitorIndex = startIndex + (slot - 9);
            if (visitorIndex < 0 || visitorIndex >= sortedVisitors.size()) {
                player.closeInventory();
                return;
            }
            UUID targetId = sortedVisitors.get(visitorIndex).getKey();
            MineVisitorsGUI.openActionGUI(player, targetId);
        }
    }
    private void handleVisitorActionGUIClick(Player player, ItemStack clickedItem, int slot, String inventoryType) {
        String[] parts = inventoryType.split(":");
        if (parts.length < 2) return;
        UUID targetId;
        try {
            targetId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            player.closeInventory();
            return;
        }
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.closeInventory();
            return;
        }
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetId);
        String targetName = targetPlayer.getName();
        if (targetName == null) targetName = "Joueur inconnu";
        Player onlineTarget = targetPlayer.isOnline() ? targetPlayer.getPlayer() : null;
        switch (slot) {
            case 10:
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    Mine ownerMine = plugin.getMineManager().getMine(player).orElse(null);
                    if (ownerMine != null && plugin.getMineManager().getMineProtectionManager().isPlayerInMineRegion(onlineTarget)) {
                        onlineTarget.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                        onlineTarget.sendMessage(ColorUtil.translateColors("&cVous avez été expulsé de la mine de &e" + player.getName() + "&c."));
                        player.sendMessage(ColorUtil.translateColors("&aVous avez expulsé &e" + targetName + "&a de votre mine."));
                    } else {
                        player.sendMessage(ColorUtil.translateColors("&cLe joueur n'est pas dans votre mine."));
                    }
                } else {
                    player.sendMessage(ColorUtil.translateColors("&cLe joueur n'est pas connecté."));
                }
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 12:
                mine.banPlayerPermanently(targetId);
                player.sendMessage(ColorUtil.translateColors("&cVous avez banni définitivement &e" + targetName + "&c de votre mine."));
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 14:
                mine.banPlayer(targetId, 3600);
                player.sendMessage(ColorUtil.translateColors("&cVous avez banni &e" + targetName + "&c de votre mine pour &e1 heure&c."));
                if (onlineTarget != null) {
                    onlineTarget.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                    onlineTarget.sendMessage(ColorUtil.translateColors("&cVous avez été banni de la mine de &e" + player.getName() + "&c pour 1 heure."));
                }
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 16:
                mine.banPlayer(targetId, 86400);
                player.sendMessage(ColorUtil.translateColors("&cVous avez banni &e" + targetName + "&c de votre mine pour &e24 heures&c."));
                if (onlineTarget != null) {
                    onlineTarget.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                    onlineTarget.sendMessage(ColorUtil.translateColors("&cVous avez été banni de la mine de &e" + player.getName() + "&c pour 24 heures."));
                }
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 20:
                mine.unbanPlayer(targetId);
                player.sendMessage(ColorUtil.translateColors("&aVous avez débanni &e" + targetName + "&a de votre mine."));
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 22:
                mine.denyAccess(targetId);
                player.sendMessage(ColorUtil.translateColors("&cVous avez refusé l'accès à &e" + targetName + "&c dans votre mine."));
                if (onlineTarget != null) {
                    onlineTarget.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                    onlineTarget.sendMessage(ColorUtil.translateColors("&cVotre accès à la mine de &e" + player.getName() + "&c a été révoqué."));
                }
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 24:
                mine.allowAccess(targetId);
                player.sendMessage(ColorUtil.translateColors("&aVous avez autorisé &e" + targetName + "&a à accéder à votre mine."));
                MineVisitorsGUI.openGUI(player, 0);
                return;
            case 31:
                MineVisitorsGUI.openGUI(player, 0);
                return;
        }
    }
    private void handleSettingsGUIClick(Player player, ItemStack clickedItem, int slot) {
        switch (slot) {
            case 10: 
                MineTypeGUI.openGUI(player, true);
                break;
            case 12: 
                player.closeInventory();
                player.sendMessage(ColorUtil.translateColors("&ePour modifier la taxe, utilisez la commande &b/jumine tax [pourcentage]"));
                break;
            case 31: 
                MineMainGUI.openGUI(player);
                break;
        }
    }
    private void handleTypeGUIClick(Player player, ItemStack clickedItem, int slot, String inventoryType, Inventory inventory) {
        boolean isChangingType = inventoryType.contains(":change");
        if (slot == inventory.getSize() - 5) {
            if (isChangingType) {
                MineSettingsGUI.openGUI(player);
            } else {
                MineMainGUI.openGUI(player);
            }
            return;
        }
        if (slot < 9 || slot >= inventory.getSize() - 9) return;
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        String type = null;
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.startsWith(ColorUtil.translateColors("&7Type: &b"))) {
                    type = line.substring(ColorUtil.translateColors("&7Type: &b").length());
                    break;
                }
            }
        }
        if (type == null) return;
        player.closeInventory();
        if (isChangingType) {
            plugin.getMineManager().setMineType(player, type);
        } else {
            plugin.getMineManager().createMine(player, type);
        }
    }
    private void handleExpandGUIClick(Player player, ItemStack clickedItem, int slot) {
        if (slot == 27) {
            MineMainGUI.openGUI(player);
            return;
        }
        if (slot == 31) {
            if (!plugin.getMineManager().hasMine(player)) {
                player.closeInventory();
                player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
                return;
            }
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine == null) return;
            int maxSize = plugin.getConfigManager().getConfig().getInt("Config.Mines.max-size", 100);
            if (mine.getSize() >= maxSize) {
                player.sendMessage(ColorUtil.translateColors("&cVotre mine a déjà atteint sa taille maximale."));
                player.closeInventory();
                return;
            }
            player.closeInventory();
            player.sendMessage(ColorUtil.translateColors("&ePour agrandir votre mine, utilisez la commande &b/jumine expand&e."));
        }
    }
    private void handleCompositionGUIClick(Player player, ItemStack clickedItem, int slot) {
        switch (slot) {
            case 48: 
                player.sendMessage(ColorUtil.translateColors("&aGénération du graphique de composition..."));
                player.sendMessage(ColorUtil.translateColors("&aGraphique affiché dans le chat:"));
                Mine mine = plugin.getMineManager().getMine(player).orElse(null);
                if (mine != null) {
                    Map<Material, Double> blocks = mine.getBlocks();
                    player.sendMessage(ColorUtil.translateColors("&8&m--------------------"));
                    player.sendMessage(ColorUtil.translateColors("&bComposition de la Mine:"));
                    blocks.entrySet().stream()
                        .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
                        .limit(6) 
                        .forEach(entry -> {
                            Material material = entry.getKey();
                            double percentage = entry.getValue();
                            int barLength = (int) Math.round(percentage / 5);
                            StringBuilder bar = new StringBuilder();
                            for (int i = 0; i < barLength; i++) {
                                bar.append("█");
                            }
                            player.sendMessage(ColorUtil.translateColors(
                                    "&b" + formatMaterialName(material.name()) + ": &a" + bar.toString() + 
                                    " &f" + String.format("%.1f", percentage) + "%"));
                        });
                    player.sendMessage(ColorUtil.translateColors("&8&m--------------------"));
                }
                break;
            case 50: 
                MineStatsGUI.openGUI(player);
                break;
        }
    }
    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                       .append(part.substring(1))
                       .append(" ");
            }
        }
        return builder.toString().trim();
    }
    private void toggleMineAccess(Player player) {
        if (!plugin.getMineManager().hasMine(player)) return;
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) return;
        boolean currentState = mine.isOpen();
        mine.setOpen(!currentState);
        plugin.getMineManager().saveMine(mine);
        String message = currentState ? 
                "&cVotre mine est maintenant fermée aux visiteurs." : 
                "&aVotre mine est maintenant ouverte aux visiteurs.";
        player.sendMessage(ColorUtil.translateColors(message));
    }
} 