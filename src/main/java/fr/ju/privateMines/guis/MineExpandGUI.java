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
import net.kyori.adventure.text.Component;
public class MineExpandGUI {
    private static final String GUI_TITLE = "&8■ &bProgression de Mine &8■";
    private static final String INVENTORY_TYPE = "mine_expand";
    private static final int[] SIZE_MILESTONES = {5, 10, 20, 50, 100};
    public static void openGUI(Player player) {
        PrivateMines plugin = PrivateMines.getInstance();
        GUIManager guiManager = plugin.getGUIManager();
        if (!plugin.getMineManager().hasMine(player)) {
            player.sendMessage(ColorUtil.translateColors("&cVous n'avez pas de mine privée."));
            return;
        }
        Optional<Mine> mineOpt = plugin.getMineManager().getMine(player);
        if (mineOpt.isEmpty()) {
            player.sendMessage(ColorUtil.translateColors("&cErreur lors de la récupération de votre mine."));
            return;
        }
        Mine mine = mineOpt.get();
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text(ColorUtil.translateColors(GUI_TITLE))); 
        int maxSize = plugin.getConfigManager().getConfig().getInt("Config.Mines.max-size", 100);
        List<String> currentLore = new ArrayList<>();
        currentLore.add("&7Votre mine est actuellement de");
        currentLore.add("&7taille &b" + mine.getSize() + "&7.");
        currentLore.add("");
        currentLore.add("&7Une mine plus grande contient");
        currentLore.add("&7plus de blocs à miner, ce qui");
        currentLore.add("&7augmente votre productivité.");
        currentLore.add("");
        currentLore.add("&7Taille maximale: &b" + maxSize);
        ItemStack currentItem = guiManager.createGuiItem(Material.GOLDEN_PICKAXE, "&e⚒ &bTaille Actuelle: &a" + mine.getSize(), currentLore);
        inventory.setItem(4, currentItem);
        int progressPercentage = (mine.getSize() * 100) / maxSize;
        String progressBar = GUIManager.createProgressBar(progressPercentage);
        List<String> progressLore = new ArrayList<>();
        progressLore.add(progressBar);
        progressLore.add("");
        progressLore.add("&7Progression: &b" + progressPercentage + "%");
        progressLore.add("&7(&b" + mine.getSize() + "&7/&b" + maxSize + "&7)");
        ItemStack progressItem = guiManager.createGuiItem(Material.EXPERIENCE_BOTTLE, "&e📊 &bProgression d'Agrandissement", progressLore);
        inventory.setItem(13, progressItem);
        int slotIndex = 19;
        for (int milestone : SIZE_MILESTONES) {
            if (slotIndex > 25) break; 
            boolean achieved = mine.getSize() >= milestone;
            boolean current = mine.getSize() == milestone;
            List<String> milestoneLore = new ArrayList<>();
            if (current) {
                milestoneLore.add("&aVous êtes à ce palier!");
            } else if (achieved) {
                milestoneLore.add("&aPalier atteint!");
            } else {
                milestoneLore.add("&7Il vous reste &b" + (milestone - mine.getSize()) + " &7niveaux");
                milestoneLore.add("&7pour atteindre ce palier.");
            }
            milestoneLore.add("");
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
            Material material;
            String name;
            if (current) {
                material = Material.EMERALD_BLOCK;
                name = "&a⭐ &bPalier " + milestone + " &a(Actuel)";
            } else if (achieved) {
                material = Material.DIAMOND_BLOCK;
                name = "&a✓ &bPalier " + milestone + " &a(Atteint)";
            } else {
                material = Material.IRON_BLOCK;
                name = "&7⬜ &bPalier " + milestone + " &7(Futur)";
            }
            ItemStack milestoneItem = guiManager.createGuiItem(material, name, milestoneLore);
            inventory.setItem(slotIndex++, milestoneItem);
        }
        List<String> expandLore = new ArrayList<>();
        if (mine.getSize() >= maxSize) {
            expandLore.add("&cVous avez atteint la taille maximale!");
            expandLore.add("&cVotre mine ne peut plus être agrandie.");
            ItemStack expandItem = guiManager.createGuiItem(Material.BARRIER, "&c❌ &7Taille Maximale Atteinte", expandLore);
            inventory.setItem(31, expandItem);
        } else {
            int costPerSize = plugin.getConfigManager().getConfig().getInt("Config.Mines.expand-cost", 100);
            expandLore.add("&7Agrandir votre mine pour augmenter");
            expandLore.add("&7sa capacité et débloquer des avantages.");
            expandLore.add("");
            expandLore.add("&7Taille actuelle: &b" + mine.getSize());
            expandLore.add("&7Prochaine taille: &b" + (mine.getSize() + 1));
            expandLore.add("");
            expandLore.add("&7Coût: &b" + costPerSize + " niveaux");
            expandLore.add("");
            expandLore.add("&eCliquez pour agrandir");
            ItemStack expandItem = guiManager.createGuiItem(Material.GOLDEN_PICKAXE, "&e↗ &bAgrandir la Mine", expandLore);
            inventory.setItem(31, expandItem);
        }
        ItemStack backButton = guiManager.createGuiItem(Material.BARRIER, "&e◀ &cRetour",
                "&7Retourner au menu principal");
        inventory.setItem(27, backButton);
        guiManager.fillEmptySlots(inventory);
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        guiManager.registerOpenInventory(player, INVENTORY_TYPE);
    }
} 