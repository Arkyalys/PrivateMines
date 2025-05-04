package fr.ju.privateMines.listeners;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.guis.MineCompositionGUI;
import fr.ju.privateMines.guis.MineExpandGUI;
import fr.ju.privateMines.guis.MineMainGUI;
import fr.ju.privateMines.guis.MineSettingsGUI;
import fr.ju.privateMines.guis.MineStatsGUI;
import fr.ju.privateMines.guis.MineVisitorsGUI;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class GUIListener implements Listener {
    private final PrivateMines plugin;
    private final Map<UUID, Boolean> awaitingContributorName = new HashMap<>();
    private final Map<UUID, Boolean> awaitingContributorChat = new HashMap<>();
    public GUIListener(PrivateMines plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        Player player = (Player) event.getWhoClicked();
        String inventoryType = plugin.getGUIManager().getOpenInventoryType(player);
        if (inventoryType == null) return;
        String type = inventoryType.split(":")[0];
        if (!type.equals("mine_contributor_anvil")) {
            event.setCancelled(true);
        }
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        switch (type) {
            case "mine_main":
                handleMainGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_stats":
                handleStatsGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_visitors":
            case "mine_contributors":
                handleVisitorsGUIClick(player, clickedItem, event.getSlot(), inventoryType, event);
                break;
            case "mine_visitor_action":
                handleVisitorActionGUIClick(player, clickedItem, event.getSlot(), inventoryType);
                break;
            case "mine_settings":
                handleSettingsGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_expand":
                handleExpandGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_composition":
                handleCompositionGUIClick(player, clickedItem, event.getSlot());
                break;
            case "mine_contributor_anvil":
                handleContributorAnvilClick(player, event);
                break;
        }
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        plugin.getGUIManager().unregisterOpenInventory(player);
    }
    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingContributorChat.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (msg.equalsIgnoreCase("/cancel")) {
            player.sendMessage(ColorUtil.translateColors("&cAjout de contributeur annulé."));
            awaitingContributorChat.remove(player.getUniqueId());
            return;
        }
        Player target = plugin.getServer().getPlayerExact(msg);
        if (target == null) {
            player.sendMessage(ColorUtil.translateColors("&cJoueur introuvable. Réessaie ou tape /cancel."));
            return;
        }
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur mine."));
            awaitingContributorChat.remove(player.getUniqueId());
            return;
        }
        mine.addContributor(target.getUniqueId());
        player.sendMessage(ColorUtil.translateColors("&aContributeur ajouté !"));
        awaitingContributorChat.remove(player.getUniqueId());
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> MineVisitorsGUI.openGUI(player, 0));
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
                    plugin.getMineManager().createMine(player);
                    break;
                case 31: 
                    if (player.hasPermission("privateMines.admin.create")) {
                        // MineTypeGUI.openGUI(player, false);
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
        player.sendMessage("[DEBUG] handleVisitorsGUIClick: slot=" + slot + ", inventoryType=" + inventoryType);
        int currentPage = 0;
        String[] parts = inventoryType.split(":");
        if (parts.length > 1) {
            try {
                currentPage = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("[DEBUG] Erreur parsing page: " + e.getMessage());
            }
        }
        if (slot == 6) {
            player.closeInventory();
            player.sendMessage(ColorUtil.translateColors("&eÉcris le pseudo du joueur à ajouter comme contributeur dans le chat. Tape &c/cancel &epour annuler."));
            awaitingContributorChat.put(player.getUniqueId(), true);
            return;
        }
        if (slot == 45 && currentPage > 0) {
            player.sendMessage("[DEBUG] Clic page précédente");
            MineVisitorsGUI.openGUI(player, currentPage - 1);
            return;
        }
        if (slot == 53) {
            player.sendMessage("[DEBUG] Clic page suivante");
            MineVisitorsGUI.openGUI(player, currentPage + 1);
            return;
        }
        if (slot == 49) {
            player.sendMessage("[DEBUG] Clic bouton retour");
            MineMainGUI.openGUI(player);
            return;
        }
        if (slot >= 9 && slot <= 44) {
            player.sendMessage("[DEBUG] Clic sur une tête de contributeur, slot=" + slot);
            PrivateMines plugin = PrivateMines.getInstance();
            Mine mine = plugin.getMineManager().getMine(player).orElse(null);
            if (mine == null) {
                player.sendMessage("[DEBUG] Mine null");
                player.closeInventory();
                return;
            }
            org.bukkit.World world = mine.getLocation().getWorld();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            String regionId = "mine-" + mine.getOwner().toString();
            com.sk89q.worldguard.protection.regions.ProtectedRegion region = regionManager != null ? regionManager.getRegion(regionId) : null;
            List<UUID> contributors = new ArrayList<>();
            if (region != null) {
                for (String uuidStr : region.getMembers().getPlayers()) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        if (!uuid.equals(mine.getOwner())) {
                            contributors.add(uuid);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            int startIndex = currentPage * MineVisitorsGUI.PAGE_SIZE;
            int contributorIndex = startIndex + (slot - 9);
            if (contributorIndex < 0 || contributorIndex >= contributors.size()) {
                player.sendMessage("[DEBUG] contributorIndex hors limite");
                player.closeInventory();
                return;
            }
            UUID targetId = contributors.get(contributorIndex);
            player.sendMessage("[DEBUG] Ouvre menu action pour UUID=" + targetId);
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
        if (slot == 15) {
            mine.removeContributor(targetId);
            player.sendMessage(ColorUtil.translateColors("&aContributeur retiré avec succès."));
            MineVisitorsGUI.openGUI(player, 0);
            return;
        }
        if (slot == 18) {
            MineVisitorsGUI.openGUI(player, 0);
            return;
        }
    }
    private void handleSettingsGUIClick(Player player, ItemStack clickedItem, int slot) {
        switch (slot) {
            case 10: 
                // MineTypeGUI.openGUI(player, true);
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
    private void handleContributorAnvilClick(Player player, InventoryClickEvent event) {
        player.sendMessage("[DEBUG] handleContributorAnvilClick: slot=" + event.getSlot());
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlot() == 2) {
            handleAnvilSlot2Click(player, event);
        }
    }
    private void handleAnvilSlot2Click(Player player, InventoryClickEvent event) {
        ItemStack result = event.getInventory().getItem(2);
        if (!isValidAnvilResult(result, player, event)) return;
        String pseudo = extractPseudo(result);
        player.sendMessage("[DEBUG] Pseudo saisi: " + pseudo);
        if (!isValidPseudo(pseudo, player, event)) return;
        Player target = plugin.getServer().getPlayerExact(pseudo.trim());
        if (!isValidTarget(target, player, event)) return;
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (!isValidMine(mine, player, event)) return;
        org.bukkit.World world = mine.getLocation().getWorld();
        com.sk89q.worldguard.protection.managers.RegionManager regionManager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
        String regionId = "mine-" + mine.getOwner().toString();
        com.sk89q.worldguard.protection.regions.ProtectedRegion region = regionManager != null ? regionManager.getRegion(regionId) : null;
        if (!isValidRegion(region, player, event)) return;
        
        if (region == null) return;
        
        region.getMembers().addPlayer(target.getUniqueId());
        player.sendMessage(ColorUtil.translateColors("&aContributeur ajouté !"));
        awaitingContributorName.remove(player.getUniqueId());
        MineVisitorsGUI.openGUI(player, 0);
        event.setCancelled(true);
        player.closeInventory();
    }
    private boolean isValidAnvilResult(ItemStack result, Player player, InventoryClickEvent event) {
        if (result == null || result.getType() != Material.PAPER) {
            player.sendMessage("[DEBUG] Pas de papier dans le slot 2");
            return false;
        }
        return true;
    }
    private String extractPseudo(ItemStack result) {
        if (result.getItemMeta() != null && result.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(result.getItemMeta().displayName());
        }
        return null;
    }
    private boolean isValidPseudo(String pseudo, Player player, InventoryClickEvent event) {
        if (pseudo == null || pseudo.trim().isEmpty() || pseudo.equals("Entrer le pseudo")) {
            player.sendMessage(ColorUtil.translateColors("&cPseudo invalide."));
            event.setCancelled(true);
            return false;
        }
        return true;
    }
    private boolean isValidTarget(Player target, Player player, InventoryClickEvent event) {
        if (target == null) {
            player.sendMessage(ColorUtil.translateColors("&cJoueur introuvable."));
            event.setCancelled(true);
            return false;
        }
        return true;
    }
    private boolean isValidMine(Mine mine, Player player, InventoryClickEvent event) {
        if (mine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur mine."));
            event.setCancelled(true);
            return false;
        }
        return true;
    }
    private boolean isValidRegion(com.sk89q.worldguard.protection.regions.ProtectedRegion region, Player player, InventoryClickEvent event) {
        if (region == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur région."));
            event.setCancelled(true);
            return false;
        }
        return true;
    }
} 