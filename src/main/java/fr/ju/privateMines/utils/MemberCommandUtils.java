package fr.ju.privateMines.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.models.Mine;

/**
 * Classe utilitaire fournissant des méthodes communes pour les commandes
 * de gestion des membres (add/remove).
 */
public class MemberCommandUtils {
    
    /**
     * Vérifie si les conditions de base sont remplies pour une commande de gestion des membres.
     * 
     * @param player Le joueur exécutant la commande
     * @param args Les arguments de la commande
     * @param mineManager Le gestionnaire de mines
     * @param configManager Le gestionnaire de configuration
     * @param usage Le message d'utilisation à afficher si les arguments sont insuffisants
     * @return Un tableau contenant la mine et la cible, ou null si une condition n'est pas remplie
     */
    public static Object[] checkMemberCommandConditions(Player player, String[] args, 
                                                      MineManager mineManager, 
                                                      ConfigManager configManager,
                                                      String usage) {
        // Vérifier les arguments
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage(usage));
            return null;
        }
        
        // Vérifier si le joueur possède une mine
        Mine mine = mineManager.getMine(player).orElse(null);
        if (mine == null) {
            player.sendMessage(configManager.getMessage("mine-not-found"));
            return null;
        }
        
        // Vérifier si la cible existe
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(configManager.getMessage("mine-invalid-player"));
            return null;
        }
        
        return new Object[] { mine, target };
    }
} 