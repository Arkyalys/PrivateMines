package fr.ju.privateMines.managers;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.ConfigManager;
public class MineTypeService {
    private final PrivateMines plugin;
    public MineTypeService(PrivateMines plugin) {
        this.plugin = plugin;
    }
    public void loadMineTypes(Map<String, Map<Material, Double>> mineTypes, ConfigManager configManager) {
        ConfigurationSection typesSection = configManager.getConfig().getConfigurationSection("Config.Mines.default.types");
        if (typesSection == null) return;
        for (String type : typesSection.getKeys(false)) {
            Map<Material, Double> blocks = new HashMap<>();
            ConfigurationSection blocksSection = typesSection.getConfigurationSection(type + ".blocks");
            if (blocksSection != null) {
                for (String materialName : blocksSection.getKeys(false)) {
                    Material material = Material.getMaterial(materialName.toUpperCase());
                    if (material != null) {
                        double chance = blocksSection.getDouble(materialName);
                        blocks.put(material, chance);
                    }
                }
            }
            mineTypes.put(type, blocks);
        }
    }
    public void loadMineTiers(Map<Integer, Map<Material, Double>> mineTiers, ConfigManager configManager, PrivateMines plugin) {
        ConfigurationSection tiersSection = configManager.getConfig().getConfigurationSection("Config.Mines.tiers");
        if (tiersSection == null) {
            Map<Material, Double> tier1 = new HashMap<>();
            tier1.put(Material.STONE, 70.0);
            tier1.put(Material.COAL_ORE, 15.0);
            tier1.put(Material.IRON_ORE, 10.0);
            tier1.put(Material.GOLD_ORE, 5.0);
            mineTiers.put(1, tier1);
            Map<Material, Double> tier2 = new HashMap<>();
            tier2.put(Material.STONE, 60.0);
            tier2.put(Material.COAL_ORE, 15.0);
            tier2.put(Material.IRON_ORE, 15.0);
            tier2.put(Material.GOLD_ORE, 8.0);
            tier2.put(Material.DIAMOND_ORE, 2.0);
            mineTiers.put(2, tier2);
            Map<Material, Double> tier3 = new HashMap<>();
            tier3.put(Material.STONE, 50.0);
            tier3.put(Material.COAL_ORE, 15.0);
            tier3.put(Material.IRON_ORE, 15.0);
            tier3.put(Material.GOLD_ORE, 10.0);
            tier3.put(Material.DIAMOND_ORE, 7.0);
            tier3.put(Material.EMERALD_ORE, 3.0);
            mineTiers.put(3, tier3);
            plugin.getLogger().info("Configurations des paliers par défaut créées");
            return;
        }
        for (String tierKey : tiersSection.getKeys(false)) {
            try {
                int tierLevel = Integer.parseInt(tierKey);
                Map<Material, Double> blocks = new HashMap<>();
                ConfigurationSection blocksSection = tiersSection.getConfigurationSection(tierKey + ".blocks");
                if (blocksSection != null) {
                    for (String materialName : blocksSection.getKeys(false)) {
                        Material material = Material.getMaterial(materialName.toUpperCase());
                        if (material != null) {
                            double chance = blocksSection.getDouble(materialName);
                            blocks.put(material, chance);
                        }
                    }
                }
                mineTiers.put(tierLevel, blocks);
                plugin.getLogger().info("Palier " + tierLevel + " chargé avec " + blocks.size() + " types de blocs");
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Format de palier invalide : " + tierKey);
            }
        }
    }
} 