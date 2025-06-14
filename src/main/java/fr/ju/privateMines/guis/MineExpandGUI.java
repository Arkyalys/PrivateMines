package fr.ju.privateMines.guis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import fr.ju.privateMines.utils.ProgressBarUtil;
import net.kyori.adventure.text.Component;

public class MineExpandGUI {
    private static final String GUI_TITLE = "&8■ &bProgression de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_expand";
    private static final int[] SIZE_MILESTONES = {5, 10, 20, 50, 100};
    
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        
        // Vérifier que le joueur a une mine
        Mine mine = validatePlayerMine(player, plugin);
        if (mine == null) {
            return;
        }
        
        // Créer l'inventaire
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text(ColorUtil.translateColors(GUI_TITLE)));
        
        // Récupérer la taille maximale de mine configurée
        int maxSize = plugin.getConfigManager().getConfig().getInt("Config.Mines.max-size", 100);
        
        // Ajouter les différents éléments à l'inventaire
        addCurrentSizeItem(inventory, mine, maxSize);
        addProgressBar(inventory, mine, maxSize);
        addMilestoneItems(inventory, mine);
        addExpandButton(inventory, mine, maxSize, plugin);
        addNavigationButtons(inventory, plugin.getGUIManager());
        
        // Finaliser et afficher l'inventaire
        plugin.getGUIManager().fillEmptySlots(inventory);
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        plugin.getGUIManager().registerOpenInventory(player, INVENTORY_TYPE);
    }
    
    /**
     * Vérifie que le joueur possède une mine valide
     * @return La mine du joueur ou null en cas d'erreur
     */
    private static Mine validatePlayerMine(Player player, PrivateMines plugin) {
        if (!plugin.getMineManager().hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
            return null;
        }
        
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(player);
        if (mineOpt.isEmpty()) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return null;
        }
        
        return mineOpt.get();
    }
    
    /**
     * Ajoute l'item d'information sur la taille actuelle de la mine
     */
    private static void addCurrentSizeItem(Inventory inventory, Mine mine, int maxSize) {
        GUIManager guiManager = PrivateMines.getInstance().getGUIManager();
        
        List<String> currentLore = new ArrayList<>();
        currentLore.add("&7Votre mine est actuellement de");
        currentLore.add("&7taille &b" + mine.getSize() + "&7.");
        currentLore.add("");
        currentLore.add("&7Une mine plus grande contient");
        currentLore.add("&7plus de blocs à miner, ce qui");
        currentLore.add("&7augmente votre productivité.");
        currentLore.add("");
        currentLore.add("&7Taille maximale: &b" + maxSize);
        
        ItemStack currentItem = guiManager.createGuiItem(
            Material.GOLDEN_PICKAXE, 
            "&e⚒ &bTaille Actuelle: &a" + mine.getSize(), 
            currentLore
        );
        
        inventory.setItem(4, currentItem);
    }
    
    /**
     * Ajoute la barre de progression de l'expansion de la mine
     */
    private static void addProgressBar(Inventory inventory, Mine mine, int maxSize) {
        GUIManager guiManager = PrivateMines.getInstance().getGUIManager();
        
        int progressPercentage = (mine.getSize() * 100) / maxSize;
        String progressBar = ProgressBarUtil.createProgressBar(progressPercentage);
        
        List<String> progressLore = new ArrayList<>();
        progressLore.add(progressBar);
        progressLore.add("");
        progressLore.add("&7Progression: &b" + progressPercentage + "%");
        progressLore.add("&7(&b" + mine.getSize() + "&7/&b" + maxSize + "&7)");
        
        ItemStack progressItem = guiManager.createGuiItem(
            Material.EXPERIENCE_BOTTLE, 
            "&e📊 &bProgression d'Agrandissement", 
            progressLore
        );
        
        inventory.setItem(13, progressItem);
    }
    
    /**
     * Ajoute les items représentant les paliers d'expansion
     */
    private static void addMilestoneItems(Inventory inventory, Mine mine) {
        // GUIManager guiManager = PrivateMines.getInstance().getGUIManager();
        int slotIndex = 19;
        for (int milestone : SIZE_MILESTONES) {
            if (slotIndex > 25) break;
            
            // Déterminer l'état du palier par rapport à la taille actuelle
            boolean achieved = mine.getSize() >= milestone;
            boolean current = mine.getSize() == milestone;
            
            // Créer la description du palier
            List<String> milestoneLore = createMilestoneLore(milestone, mine.getSize(), current, achieved);
            
            // Déterminer l'apparence de l'item en fonction de l'état
            ItemStack milestoneItem = createMilestoneItem(milestone, current, achieved, milestoneLore);
            
            // Ajouter à l'inventaire
            inventory.setItem(slotIndex++, milestoneItem);
        }
    }
    
    /**
     * Crée la description d'un palier d'expansion
     */
    private static List<String> createMilestoneLore(int milestone, int currentSize, boolean isCurrent, boolean isAchieved) {
        List<String> milestoneLore = new ArrayList<>();
        
        // Status du palier
        if (isCurrent) {
            milestoneLore.add("&aVous êtes à ce palier!");
        } else if (isAchieved) {
            milestoneLore.add("&aPalier atteint!");
        } else {
            milestoneLore.add("&7Il vous reste &b" + (milestone - currentSize) + " &7niveaux");
            milestoneLore.add("&7pour atteindre ce palier.");
        }
        
        milestoneLore.add("");
        
        // Avantage du palier
        switch (milestone) {
            case 5:
                milestoneLore.add("&7Avantage: &bDébloque le reset automatique");
                break;
            case 10:
                milestoneLore.add("&7Avantage: &b+5% de minerais rares");
                break;
            case 20:
                milestoneLore.add("&7Avantage: &bTemps de reset réduit");
                break;
            case 50:
                milestoneLore.add("&7Avantage: &bDébloque les invitations VIP");
                break;
            case 100:
                milestoneLore.add("&7Avantage: &bDébloque le statut de mine légendaire");
                break;
            default:
                milestoneLore.add("&7Avantage: &bAugmente la productivité");
        }
        
        return milestoneLore;
    }
    
    /**
     * Crée un item représentant un palier d'expansion
     */
    private static ItemStack createMilestoneItem(int milestone, boolean isCurrent, boolean isAchieved, List<String> lore) {
        GUIManager guiManager = PrivateMines.getInstance().getGUIManager();
        
        Material material;
        String name;
        
        if (isCurrent) {
            material = Material.EMERALD_BLOCK;
            name = "&a⭐ &bPalier " + milestone + " &a(Actuel)";
        } else if (isAchieved) {
            material = Material.DIAMOND_BLOCK;
            name = "&a✓ &bPalier " + milestone + " &a(Atteint)";
        } else {
            material = Material.IRON_BLOCK;
            name = "&7⬜ &bPalier " + milestone + " &7(Futur)";
        }
        
        return guiManager.createGuiItem(material, name, lore);
    }
    
    /**
     * Ajoute le bouton d'expansion de la mine
     */
    private static void addExpandButton(Inventory inventory, Mine mine, int maxSize, PrivateMines plugin) {
        GUIManager guiManager = plugin.getGUIManager();
        
        if (mine.getSize() >= maxSize) {
            // Mine à taille maximale
            addMaxSizeReachedButton(inventory, guiManager);
        } else {
            // Mine encore expandable
            addExpandActionButton(inventory, mine, plugin, guiManager);
        }
    }
    
    /**
     * Ajoute le bouton indiquant que la taille maximale est atteinte
     */
    private static void addMaxSizeReachedButton(Inventory inventory, GUIManager guiManager) {
        List<String> lore = new ArrayList<>();
        lore.add("&cVous avez atteint la taille maximale!");
        lore.add("&cVotre mine ne peut plus être agrandie.");
        
        ItemStack expandItem = guiManager.createGuiItem(
            Material.BARRIER, 
            "&c❌ &7Taille Maximale Atteinte", 
            lore
        );
        
        inventory.setItem(31, expandItem);
    }
    
    /**
     * Ajoute le bouton d'action pour agrandir la mine
     */
    private static void addExpandActionButton(Inventory inventory, Mine mine, PrivateMines plugin, GUIManager guiManager) {
        int costPerSize = plugin.getConfigManager().getConfig().getInt("Config.Mines.expand-cost", 100);
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Agrandir votre mine pour augmenter");
        lore.add("&7sa capacité et débloquer des avantages.");
        lore.add("");
        lore.add("&7Taille actuelle: &b" + mine.getSize());
        lore.add("&7Prochaine taille: &b" + (mine.getSize() + 1));
        lore.add("");
        lore.add("&7Coût: &b" + costPerSize + " niveaux");
        lore.add("");
        lore.add("&eCliquez pour agrandir");
        
        ItemStack expandItem = guiManager.createGuiItem(
            Material.GOLDEN_PICKAXE, 
            "&e↗ &bAgrandir la Mine", 
            lore
        );
        
        inventory.setItem(31, expandItem);
    }
    
    /**
     * Ajoute les boutons de navigation
     */
    private static void addNavigationButtons(Inventory inventory, GUIManager guiManager) {
        ItemStack backButton = guiManager.createGuiItem(
            Material.BARRIER, 
            "&e◀ &cRetour",
            "&7Retourner au menu principal"
        );
        
        inventory.setItem(27, backButton);
    }
} 