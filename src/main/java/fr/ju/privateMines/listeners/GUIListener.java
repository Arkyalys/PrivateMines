package fr.ju.privateMines.listeners;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

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
    
    // Map associant chaque type d'inventaire à son gestionnaire de clic
    private final Map<String, BiConsumer<Player, InventoryClickContext>> clickHandlers = new HashMap<>();
    
    public GUIListener(PrivateMines plugin) {
        this.plugin = plugin;
        initializeClickHandlers();
    }
    
    /**
     * Initialise la map des gestionnaires de clic pour chaque type d'inventaire
     */
    private void initializeClickHandlers() {
        clickHandlers.put("mine_main", (player, context) -> 
            handleMainGUIClick(player, context.clickedItem, context.slot));
            
        clickHandlers.put("mine_stats", (player, context) -> 
            handleStatsGUIClick(player, context.clickedItem, context.slot));
            
        clickHandlers.put("mine_visitors", (player, context) -> 
            handleVisitorsGUIClick(player, context.clickedItem, context.slot, context.inventoryType, context.event));
            
        clickHandlers.put("mine_contributors", (player, context) -> 
            handleVisitorsGUIClick(player, context.clickedItem, context.slot, context.inventoryType, context.event));
            
        clickHandlers.put("mine_visitor_action", (player, context) -> 
            handleVisitorActionGUIClick(player, context.clickedItem, context.slot, context.inventoryType));
            
        clickHandlers.put("mine_settings", (player, context) -> 
            handleSettingsGUIClick(player, context.clickedItem, context.slot));
            
        clickHandlers.put("mine_expand", (player, context) -> 
            handleExpandGUIClick(player, context.clickedItem, context.slot));
            
        clickHandlers.put("mine_composition", (player, context) -> 
            handleCompositionGUIClick(player, context.clickedItem, context.slot));
            
        clickHandlers.put("mine_contributor_anvil", (player, context) -> 
            handleContributorAnvilClick(player, context.event));
    }
    
    /**
     * Classe pour regrouper les informations de contexte d'un clic d'inventaire
     */
    private static class InventoryClickContext {
        final ItemStack clickedItem;
        final int slot;
        final String inventoryType;
        final InventoryClickEvent event;
        
        InventoryClickContext(ItemStack clickedItem, int slot, String inventoryType, InventoryClickEvent event) {
            this.clickedItem = clickedItem;
            this.slot = slot;
            this.inventoryType = inventoryType;
            this.event = event;
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Vérifications préliminaires
        if (!isValidInventoryClick(event)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Déterminer le type d'inventaire
        String inventoryInfo = plugin.getGUIManager().getOpenInventoryType(player);
        if (inventoryInfo == null) {
            return;
        }
        
        // Extraire le type de base de l'inventaire (première partie avant les ':')
        String baseType = extractBaseType(inventoryInfo);
        
        // Vérifier si c'est un inventaire spécial qui ne doit pas annuler les clics
        if (!baseType.equals("mine_contributor_anvil")) {
            event.setCancelled(true);
        }
        
        // Vérifier l'item cliqué
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Trouver et exécuter le gestionnaire approprié
        BiConsumer<Player, InventoryClickContext> handler = clickHandlers.get(baseType);
        if (handler != null) {
            InventoryClickContext context = new InventoryClickContext(
                clickedItem, event.getSlot(), inventoryInfo, event);
            handler.accept(player, context);
        }
    }
    
    /**
     * Vérifie si un clic d'inventaire est valide pour le traitement
     */
    private boolean isValidInventoryClick(InventoryClickEvent event) {
        return event.getClickedInventory() != null && event.getWhoClicked() instanceof Player;
    }
    
    /**
     * Extrait le type de base d'un identifiant d'inventaire (partie avant les ':')
     */
    private String extractBaseType(String inventoryInfo) {
        String[] parts = inventoryInfo.split(":");
        return parts[0];
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
            handleOwnerMainGUIClick(player, slot);
        } else {
            handleNonOwnerMainGUIClick(player, slot);
        }
    }
    
    private void handleOwnerMainGUIClick(Player player, int slot) {
        // Utilisation d'une approche fonctionnelle avec une map de slots vers des actions
        Map<Integer, Runnable> slotActions = createSlotActionsMap(player);
        
        // Exécution de l'action associée au slot s'il existe
        Runnable action = slotActions.get(slot);
        if (action != null) {
            action.run();
        }
    }
    
    /**
     * Crée une map associant chaque slot à son action correspondante
     */
    private Map<Integer, Runnable> createSlotActionsMap(Player player) {
        Map<Integer, Runnable> actions = new HashMap<>();
        
        actions.put(4, () -> handleStatsButton(player));
        actions.put(20, () -> handleTeleportButton(player));
        actions.put(21, () -> handleResetButton(player));
        actions.put(22, () -> handleToggleAccessButton(player));
        actions.put(23, () -> handleSettingsButton(player));
        actions.put(24, () -> handleVisitorsButton(player));
        actions.put(30, () -> handleExpandButton(player));
        actions.put(31, () -> handleUpgradeButton(player));
        actions.put(32, () -> handleDeleteButton(player));
        
        return actions;
    }
    
    /**
     * Gère le clic sur le bouton des statistiques
     */
    private void handleStatsButton(Player player) {
        MineStatsGUI.openGUI(player);
    }
    
    /**
     * Gère le clic sur le bouton de téléportation
     */
    private void handleTeleportButton(Player player) {
        player.closeInventory();
        plugin.getMineManager().teleportToMine(player, player);
        player.sendMessage(ColorUtil.translateColors("&aTéléportation à votre mine..."));
    }
    
    /**
     * Gère le clic sur le bouton de réinitialisation
     */
    private void handleResetButton(Player player) {
        player.closeInventory();
        plugin.getMineManager().resetMine(player);
    }
    
    /**
     * Gère le clic sur le bouton de basculement d'accès
     */
    private void handleToggleAccessButton(Player player) {
        toggleMineAccess(player);
        MineMainGUI.openGUI(player);
    }
    
    /**
     * Gère le clic sur le bouton des paramètres
     */
    private void handleSettingsButton(Player player) {
        MineSettingsGUI.openGUI(player);
    }
    
    /**
     * Gère le clic sur le bouton des visiteurs
     */
    private void handleVisitorsButton(Player player) {
        MineVisitorsGUI.openGUI(player, 0);
    }
    
    /**
     * Gère le clic sur le bouton d'expansion
     */
    private void handleExpandButton(Player player) {
        MineExpandGUI.openGUI(player);
    }
    
    /**
     * Gère le clic sur le bouton d'amélioration
     */
    private void handleUpgradeButton(Player player) {
        boolean upgradeResult = plugin.getMineManager().upgradeMine(player);
        player.closeInventory();
        if (!upgradeResult) {
            MineMainGUI.openGUI(player);
        }
        // Le message de succès est géré par la méthode upgradeMine
    }
    
    /**
     * Gère le clic sur le bouton de suppression
     */
    private void handleDeleteButton(Player player) {
        player.closeInventory();
        player.sendMessage(ColorUtil.translateColors("&c⚠ Pour confirmer la suppression de votre mine, tapez &e/jumine delete&c."));
    }
    
    private void handleNonOwnerMainGUIClick(Player player, int slot) {
        switch (slot) {
            case 22: // Create mine
                player.closeInventory();
                plugin.getMineManager().createMine(player);
                break;
            case 31: // Admin create (with type selection)
                if (player.hasPermission("privateMines.admin.create")) {
                    // MineTypeGUI.openGUI(player, false);
                }
                break;
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
        int currentPage = extractCurrentPage(inventoryType);
        
        // Gestion des boutons de navigation
        if (handleNavigationButtons(player, slot, currentPage)) {
            return;
        }
        
        // Gestion du bouton d'ajout de contributeur
        if (slot == 6) {
            handleAddContributorButton(player);
            return;
        }
        
        // Gestion d'un clic sur un contributeur
        if (slot >= 9 && slot <= 44) {
            handleContributorClick(player, slot, currentPage);
        }
    }
    
    private int extractCurrentPage(String inventoryType) {
        int currentPage = 0;
        String[] parts = inventoryType.split(":");
        if (parts.length > 1) {
            try {
                currentPage = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Utiliser la page par défaut (0) en cas d'erreur
            }
        }
        return currentPage;
    }
    
    private boolean handleNavigationButtons(Player player, int slot, int currentPage) {
        // Bouton page précédente
        if (slot == 45 && currentPage > 0) {
            MineVisitorsGUI.openGUI(player, currentPage - 1);
            return true;
        }
        
        // Bouton page suivante
        if (slot == 53) {
            MineVisitorsGUI.openGUI(player, currentPage + 1);
            return true;
        }
        
        // Bouton retour
        if (slot == 49) {
            MineMainGUI.openGUI(player);
            return true;
        }
        
        return false;
    }
    
    private void handleAddContributorButton(Player player) {
        player.closeInventory();
        player.sendMessage(ColorUtil.translateColors("&eÉcris le pseudo du joueur à ajouter comme contributeur dans le chat. Tape &c/cancel &epour annuler."));
        awaitingContributorChat.put(player.getUniqueId(), true);
    }
    
    private void handleContributorClick(Player player, int slot, int currentPage) {
        Mine mine = getMineForPlayer(player);
        if (mine == null) {
            return;
        }
        
        List<UUID> contributors = getContributorsForMine(mine);
        if (contributors.isEmpty()) {
            return;
        }
        
        int startIndex = currentPage * MineVisitorsGUI.PAGE_SIZE;
        int contributorIndex = startIndex + (slot - 9);
        
        if (contributorIndex < 0 || contributorIndex >= contributors.size()) {
            player.closeInventory();
            return;
        }
        
        UUID targetId = contributors.get(contributorIndex);
        MineVisitorsGUI.openActionGUI(player, targetId);
    }
    
    private Mine getMineForPlayer(Player player) {
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.closeInventory();
        }
        return mine;
    }
    
    private List<UUID> getContributorsForMine(Mine mine) {
        List<UUID> contributors = new ArrayList<>();
        
        try {
            org.bukkit.World world = mine.getLocation().getWorld();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = 
                com.sk89q.worldguard.WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
                    
            String regionId = "mine-" + mine.getOwner().toString();
            com.sk89q.worldguard.protection.regions.ProtectedRegion region = 
                regionManager != null ? regionManager.getRegion(regionId) : null;
                
            if (region != null) {
                for (String uuidStr : region.getMembers().getPlayers()) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        if (!uuid.equals(mine.getOwner())) {
                            contributors.add(uuid);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Ignorer les UUID invalides
                    }
                }
            }
        } catch (Exception e) {
            // Gérer les exceptions potentielles lors de l'accès à WorldGuard
        }
        
        return contributors;
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
        if (event.getInventory().getType() != InventoryType.ANVIL || event.getSlot() != 2) {
            return;
        }
        
        ItemStack result = event.getInventory().getItem(2);
        if (result == null || result.getType() != Material.PAPER) {
            return;
        }
        
        // Essayer d'ajouter le contributeur
        if (tryAddContributor(player, result)) {
            // Succès - nettoyer l'UI
            event.setCancelled(true);
            player.closeInventory();
            MineVisitorsGUI.openGUI(player, 0);
        } else {
            // Échec - annuler le clic uniquement
            event.setCancelled(true);
        }
    }
    
    /**
     * Tente d'ajouter un contributeur à la mine du joueur
     * @return true si l'ajout a réussi, false sinon
     */
    private boolean tryAddContributor(Player player, ItemStack result) {
        // Extraire le nom du joueur
        String targetName = extractTargetName(result);
        if (targetName == null || targetName.trim().isEmpty() || targetName.equals("Entrer le pseudo")) {
            player.sendMessage(ColorUtil.translateColors("&cPseudo invalide."));
            return false;
        }
        
        // Trouver le joueur cible
        Player target = plugin.getServer().getPlayerExact(targetName.trim());
        if (target == null) {
            player.sendMessage(ColorUtil.translateColors("&cJoueur introuvable."));
            return false;
        }
        
        // Trouver la mine du joueur
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine."));
            return false;
        }
        
        // Accéder à la région WorldGuard
        com.sk89q.worldguard.protection.regions.ProtectedRegion region = getPlayerMineRegion(player, mine);
        if (region == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de l'accès à la région de votre mine."));
            return false;
        }
        
        // Ajouter le joueur à la région
        region.getMembers().addPlayer(target.getUniqueId());
        player.sendMessage(ColorUtil.translateColors("&aContributeur ajouté !"));
        awaitingContributorName.remove(player.getUniqueId());
        
        return true;
    }
    
    /**
     * Extrait le nom du joueur à partir d'un ItemStack
     */
    private String extractTargetName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return null;
    }
    
    /**
     * Récupère la région WorldGuard associée à la mine du joueur
     */
    private com.sk89q.worldguard.protection.regions.ProtectedRegion getPlayerMineRegion(Player player, Mine mine) {
        try {
            org.bukkit.World world = mine.getLocation().getWorld();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = 
                com.sk89q.worldguard.WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
                    
            String regionId = "mine-" + mine.getOwner().toString();
            return regionManager != null ? regionManager.getRegion(regionId) : null;
        } catch (Exception e) {
            return null;
        }
    }
} 