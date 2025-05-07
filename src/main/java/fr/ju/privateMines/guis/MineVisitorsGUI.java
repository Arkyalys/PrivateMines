package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import net.kyori.adventure.text.Component;

public class MineVisitorsGUI {
    private static final String GUI_TITLE = "&8■ &bContributeurs de la Mine &8■";
    private static final String INVENTORY_TYPE = "mine_contributors";
    private static final String ACTION_TYPE = "mine_contributor_action";
    public static final int PAGE_SIZE = 36;

    public static void openGUI(Player player, int page) {
        PrivateMines plugin = PrivateMines.getInstance();
        
        // Vérifier que le joueur a bien une mine
        Mine mine = validatePlayerMine(player, plugin);
        if (mine == null) {
            return;
        }
        
        // Préparer les données de pagination
        List<UUID> contributors = new ArrayList<>(mine.getContributors());
        PaginationInfo paginationInfo = calculatePagination(contributors.size(), page);
        
        // Créer l'inventaire
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(ColorUtil.translateColors(GUI_TITLE)));
        
        // Ajouter les différents éléments à l'inventaire
        addInfoItem(inventory, paginationInfo, contributors.size());
        addContributorItems(inventory, contributors, paginationInfo.startIndex, paginationInfo.endIndex);
        addNavigationButtons(inventory, plugin.getGUIManager(), paginationInfo);
        addActionButtons(inventory, plugin.getGUIManager());
        
        // Compléter et afficher l'inventaire
        plugin.getGUIManager().fillEmptySlots(inventory);
        player.openInventory(inventory);
        plugin.getGUIManager().registerOpenInventory(player, INVENTORY_TYPE + ":" + paginationInfo.currentPage);
    }
    
    /**
     * Vérifie que le joueur possède une mine valide
     * @return La mine du joueur ou null si elle n'existe pas
     */
    private static Mine validatePlayerMine(Player player, PrivateMines plugin) {
        if (!plugin.getMineManager().hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
            return null;
        }
        
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return null;
        }
        
        return mine;
    }
    
    /**
     * Représente les informations de pagination pour l'interface
     */
    private static class PaginationInfo {
        final int totalPages;
        final int currentPage;
        final int startIndex;
        final int endIndex;
        
        PaginationInfo(int totalItems, int totalPages, int currentPage, int startIndex, int endIndex) {
            this.totalPages = totalPages;
            this.currentPage = currentPage;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
    
    /**
     * Calcule les informations de pagination
     */
    private static PaginationInfo calculatePagination(int totalItems, int requestedPage) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_SIZE));
        
        // Ajuster la page si nécessaire
        int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
        
        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalItems);
        
        return new PaginationInfo(totalItems, totalPages, currentPage, startIndex, endIndex);
    }
    
    /**
     * Ajoute l'élément d'information en haut de l'inventaire
     */
    private static void addInfoItem(Inventory inventory, PaginationInfo pagination, int totalContributors) {
        GUIManager guiManager = PrivateMines.getInstance().getGUIManager();
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Nombre de contributeurs: &b" + totalContributors);
        infoLore.add("");
        infoLore.add("&7Page: &b" + (pagination.currentPage + 1) + "&7/&b" + pagination.totalPages);
        
        ItemStack infoItem = guiManager.createGuiItem(Material.BOOK, "&e👥 &bContributeurs", infoLore);
        inventory.setItem(4, infoItem);
    }
    
    /**
     * Ajoute les têtes des contributeurs à l'inventaire
     */
    private static void addContributorItems(Inventory inventory, List<UUID> contributors, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            UUID contributorId = contributors.get(i);
            int slot = 9 + (i - startIndex);
            
            ItemStack contributorItem = createContributorHeadItem(contributorId);
            inventory.setItem(slot, contributorItem);
        }
    }
    
    /**
     * Crée un ItemStack tête de joueur pour un contributeur
     */
    private static ItemStack createContributorHeadItem(UUID contributorId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(contributorId);
        String name = offlinePlayer.getName();
        if (name == null) {
            name = contributorId.toString().substring(0, 8);
        }
        
        ItemStack contributorItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) contributorItem.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(ColorUtil.translateColors("&b" + name)));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.translateColors("&7Contributeur de la mine"));
            lore.add("");
            lore.add(ColorUtil.translateColors("&cCliquez pour retirer ce contributeur"));
            
            List<Component> loreComponents = lore.stream()
                .map(Component::text)
                .collect(Collectors.toList());
                
            meta.lore(loreComponents);
            meta.setOwningPlayer(offlinePlayer);
            contributorItem.setItemMeta(meta);
        }
        
        return contributorItem;
    }
    
    /**
     * Ajoute les boutons de navigation (page précédente, suivante, retour)
     */
    private static void addNavigationButtons(Inventory inventory, GUIManager guiManager, PaginationInfo pagination) {
        // Bouton page précédente
        if (pagination.currentPage > 0) {
            ItemStack previousItem = guiManager.createGuiItem(Material.ARROW, 
                                                            "&e◀ &bPage précédente", 
                                                            "&7Aller à la page " + pagination.currentPage);
            inventory.setItem(45, previousItem);
        }
        
        // Bouton retour
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, 
                                                    "&e◀ &cRetour", 
                                                    "&7Retourner au menu principal");
        inventory.setItem(49, backItem);
        
        // Bouton page suivante
        if (pagination.currentPage < pagination.totalPages - 1) {
            ItemStack nextItem = guiManager.createGuiItem(Material.ARROW, 
                                                        "&e▶ &bPage suivante", 
                                                        "&7Aller à la page " + (pagination.currentPage + 2));
            inventory.setItem(53, nextItem);
        }
    }
    
    /**
     * Ajoute les boutons d'action (ajouter contributeur)
     */
    private static void addActionButtons(Inventory inventory, GUIManager guiManager) {
        // Bouton pour ajouter un contributeur
        ItemStack addItem = guiManager.createGuiItem(Material.EMERALD, 
                                                    "&aAjouter un contributeur", 
                                                    Arrays.asList(
                                                        "&7Cliquez pour ajouter un joueur comme contributeur.", 
                                                        "&7Un menu s'ouvrira pour entrer le pseudo."
                                                    ));
        inventory.setItem(6, addItem);
    }

    public static void openActionGUI(Player player, UUID targetId) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        
        // Récupérer les informations sur le joueur cible
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetId);
        String targetName = targetPlayer.getName();
        if (targetName == null) {
            targetName = targetId.toString().substring(0, 8);
        }
        
        // Vérifier la mine du joueur
        Mine ownerMine = plugin.getMineManager().getMine(player).orElse(null);
        if (ownerMine == null) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return;
        }
        
        // Créer l'inventaire d'actions
        Inventory inventory = Bukkit.createInventory(null, 27, 
                                                    Component.text(ColorUtil.translateColors(
                                                        "&8■ &bContributeur: " + targetName + " &8■")));
        
        // Ajouter la tête du joueur cible
        ItemStack playerInfo = createPlayerInfoItem(targetPlayer, targetName);
        inventory.setItem(13, playerInfo);
        
        // Ajouter les boutons d'action
        ItemStack removeItem = guiManager.createGuiItem(Material.BARRIER, 
                                                        "&cRetirer ce contributeur", 
                                                        Arrays.asList(
                                                            "&7Cliquez pour retirer ce joueur de la liste des contributeurs."
                                                        ));
        inventory.setItem(15, removeItem);
        
        // Ajouter le bouton retour
        ItemStack backItem = guiManager.createGuiItem(Material.BARRIER, 
                                                    "&e◀ &cRetour", 
                                                    Arrays.asList(
                                                        "&7Retourner à la liste des contributeurs"
                                                    ));
        inventory.setItem(18, backItem);
        
        // Compléter et afficher l'inventaire
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, ACTION_TYPE + ":" + targetId.toString());
    }
    
    /**
     * Crée un ItemStack avec la tête du joueur pour le menu d'actions
     */
    private static ItemStack createPlayerInfoItem(OfflinePlayer player, String playerName) {
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerInfo.getItemMeta();
        
        if (playerMeta != null) {
            playerMeta.displayName(Component.text(ColorUtil.translateColors("&b" + playerName)));
            
            List<String> playerLore = new ArrayList<>();
            playerLore.add(ColorUtil.translateColors("&7Contributeur de la mine"));
            playerLore.add("");
            playerLore.add(ColorUtil.translateColors("&cCliquez pour retirer ce contributeur"));
            
            List<Component> playerLoreComponents = playerLore.stream()
                .map(Component::text)
                .collect(Collectors.toList());
                
            playerMeta.lore(playerLoreComponents);
            playerMeta.setOwningPlayer(player);
            playerInfo.setItemMeta(playerMeta);
        }
        
        return playerInfo;
    }
} 