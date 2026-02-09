package fr.ju.privateMines.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.ju.privateMines.managers.MineManager;
import fr.ju.privateMines.utils.ConfigManager;
import fr.ju.privateMines.utils.Permissions;
public class MineTaxCommand implements SubCommand {
    private final MineManager mineManager;
    private final ConfigManager configManager;
    public MineTaxCommand(MineManager mineManager, ConfigManager configManager) {
        this.mineManager = mineManager;
        this.configManager = configManager;
    }
    @Override
    public boolean execute(Player player, String[] args, CommandSender sender, Command command, String label) {
        if (!player.hasPermission(Permissions.SET_TAX)) {
            player.sendMessage(configManager.getMessage("mine-no-permission"));
            return true;
        }
        if (args.length < 2) {
            Map<String, String> replacements = new HashMap<>();
            int maxTax = configManager.getMaxTax();
            replacements.put("%max-tax%", String.valueOf(maxTax));
            player.sendMessage(configManager.getMessage("mine-invalid-tax", replacements));
            return true;
        }
        try {
            int tax = Integer.parseInt(args[1]);
            mineManager.setMineTax(player, tax);
        } catch (NumberFormatException e) {
            Map<String, String> replacements = new HashMap<>();
            int maxTax = configManager.getMaxTax();
            replacements.put("%max-tax%", String.valueOf(maxTax));
            player.sendMessage(configManager.getMessage("mine-invalid-tax", replacements));
        }
        return true;
    }
} 