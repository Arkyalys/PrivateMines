package fr.ju.privateMines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.guis.MineMainGUI;
import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;

public class MineGuiCommand implements SubCommand {
    // Cette commande n'a pas besoin de champs, car elle utilise seulement MineMainGUI.openGUI

    public MineGuiCommand(MineManager mineManager, ConfigManager configManager, PrivateMines plugin) {
        // Constructeur vide, conservé pour compatibilité avec l'interface SubCommand
    }

    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        MineMainGUI.openGUI(player);
        return true;
    }
} 