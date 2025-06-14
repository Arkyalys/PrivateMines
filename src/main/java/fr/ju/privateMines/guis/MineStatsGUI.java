package fr.ju.privateMines.guis;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.models.Mine;
import fr.ju.privateMines.models.MineStats;
import fr.ju.privateMines.utils.ColorUtil;
import fr.ju.privateMines.utils.GUIManager;
import fr.ju.privateMines.utils.ProgressBarUtil;
import net.kyori.adventure.text.Component;

public class MineStatsGUI {
    private static final String GUI_TITLE = "&8■ &bStatistiques de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_stats";
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        
        if (!plugin.getMineManager().hasMine(player)) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("gui.no-mine", "&cVous n'avez pas de mine privée."));
            return;
        }
        
        Mine mine = plugin.getMineManager().getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(plugin.getConfigManager().getMessageOrDefault("gui.mine-error", "&cErreur lors de la récupération de votre mine."));
            return;
        }
        
        MineStats stats = mine.getStats();
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text(ColorUtil.translateColors(GUI_TITLE))); 
        
        // Ajout des différents éléments d'interface
        addGeneralInfoItem(inventory, guiManager, mine);
        addProgressItem(inventory, guiManager, stats);
        addResetInfoItem(inventory, guiManager, stats);
        addVisitorInfoItem(inventory, guiManager, stats);
        addBlocksInfoItem(inventory, guiManager, mine);
        
        // Bouton de retour
        addBackButton(inventory, guiManager);
        
        // Remplissage des emplacements vides
        guiManager.fillEmptySlots(inventory);
        
        // Ouverture de l'inventaire
        player.openInventory(inventory);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE);
    }

    private static void addGeneralInfoItem(Inventory inventory, GUIManager guiManager, Mine mine) {
        List<String> generalLore = new ArrayList<>();
        generalLore.add("&7Palier: &b" + mine.getTier());
        generalLore.add("&7Taille: &b" + mine.getSize());
        generalLore.add("&7Taxe: &b" + mine.getTax() + "%");
        generalLore.add("&7État: " + (mine.isOpen() ? "&aOuverte" : "&cFermée"));
        
        ItemStack generalItem = guiManager.createGuiItem(
            Material.GOLDEN_PICKAXE, 
            "&e⚒ &bInformations Générales", 
            generalLore
        );
        
        inventory.setItem(10, generalItem);
    }

    private static void addProgressItem(Inventory inventory, GUIManager guiManager, MineStats stats) {
        List<String> progressLore = new ArrayList<>();
        progressLore.add("&7Blocs minés: &b" + stats.getBlocksMined() + "&7/&b" + stats.getTotalBlocks());
        progressLore.add("&7Progression: &b" + stats.getPercentageMined() + "%");
        progressLore.add("");
        progressLore.add(ProgressBarUtil.createProgressBar(stats.getPercentageMined()));
        
        ItemStack progressItem = guiManager.createGuiItem(
            Material.DIAMOND_PICKAXE, 
            "&e📊 &bProgression du Minage", 
            progressLore
        );
        
        inventory.setItem(12, progressItem);
    }

    private static void addResetInfoItem(Inventory inventory, GUIManager guiManager, MineStats stats) {
        List<String> resetLore = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        long lastReset = stats.getLastReset();
        String resetDate = lastReset > 0 ? sdf.format(new Date(lastReset)) : "Jamais";
        
        resetLore.add("&7Dernier reset: &b" + resetDate);
        
        if (lastReset > 0) {
            resetLore.add("&7Temps écoulé: &b" + formatTimeElapsed(lastReset));
        }
        
        ItemStack resetItem = guiManager.createGuiItem(
            Material.CLOCK, 
            "&e🔄 &bDernier Reset", 
            resetLore
        );
        
        inventory.setItem(14, resetItem);
    }

    private static String formatTimeElapsed(long lastReset) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastReset;
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        hours %= 24;
        minutes %= 60;
        seconds %= 60;
        
        return days + "j " + hours + "h " + minutes + "m " + seconds + "s";
    }

    private static void addVisitorInfoItem(Inventory inventory, GUIManager guiManager, MineStats stats) {
        List<String> visitorLore = new ArrayList<>();
        visitorLore.add("&7Nombre total de visites: &b" + stats.getVisits());
        visitorLore.add("&7Visiteurs uniques: &b" + stats.getVisitorStats().size());
        visitorLore.add("");
        visitorLore.add("&bTop 3 des visiteurs:");
        
        Map<java.util.UUID, Integer> visitorStats = stats.getVisitorStats();
        visitorStats.entrySet().stream()
            .sorted(Map.Entry.<java.util.UUID, Integer>comparingByValue().reversed())
            .limit(3)
            .forEach(entry -> {
                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (playerName == null) playerName = "Joueur inconnu";
                visitorLore.add("&7- &b" + playerName + "&7: &b" + entry.getValue() + " visites");
            });
        
        visitorLore.add("");
        visitorLore.add("&eCliquez pour voir tous les visiteurs");
        
        ItemStack visitorItem = guiManager.createGuiItem(
            Material.PLAYER_HEAD, 
            "&e👥 &bVisiteurs", 
            visitorLore
        );
        
        inventory.setItem(16, visitorItem);
    }

    private static void addBlocksInfoItem(Inventory inventory, GUIManager guiManager, Mine mine) {
        List<String> blocksLore = new ArrayList<>();
        blocksLore.add("&7Types de blocs dans la mine:");
        
        Map<Material, Double> blocks = mine.getBlocks();
        blocks.entrySet().stream()
            .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
            .limit(5) 
            .forEach(entry -> {
                String blockName = GUIManager.formatMaterialName(entry.getKey().name());
                double percentage = entry.getValue();
                blocksLore.add("&7- &b" + blockName + "&7: &b" + String.format("%.1f", percentage) + "%");
            });
        
        if (blocks.size() > 5) {
            blocksLore.add("&7...");
            blocksLore.add("&7+" + (blocks.size() - 5) + " autres types de blocs");
        }
        
        blocksLore.add("");
        blocksLore.add("&eCliquez pour voir tous les détails");
        
        ItemStack blocksItem = guiManager.createGuiItem(
            Material.STONE, 
            "&e🧱 &bComposition de la Mine", 
            blocksLore
        );
        
        inventory.setItem(22, blocksItem);
    }

    private static void addBackButton(Inventory inventory, GUIManager guiManager) {
        ItemStack backButton = guiManager.createGuiItem(
            Material.BARRIER, 
            "&e◀ &cRetour",
            "&7Retourner au menu principal"
        );
        
        inventory.setItem(31, backButton);
    }
} 