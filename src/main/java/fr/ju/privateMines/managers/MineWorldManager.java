package fr.ju.privateMines.managers;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import fr.ju.privateMines.PrivateMines;
import fr.ju.privateMines.utils.ConfigManager;
public class MineWorldManager {
    private final PrivateMines plugin;
    private final ConfigManager configManager;
    private World mineWorld;
    private final String worldName = "mines";
    private int mineCounter; 
    private List<Location> freeLocations;
    public MineWorldManager(PrivateMines plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.freeLocations = new ArrayList<>();
        this.mineCounter = configManager.getData().getInt("world.mine_counter", 0);
        createOrLoadWorld();
    }
    private void createOrLoadWorld() {
        mineWorld = plugin.getServer().getWorld(worldName);
        if (mineWorld == null) {
            plugin.getLogger().info("Création du monde des mines...");
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generatorSettings("{\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}], \"structures\": {\"structures\": {}}}");
            mineWorld = creator.createWorld();
            if (mineWorld != null) {
                mineWorld.setSpawnLocation(0, 64, 0);
                mineWorld.setTime(6000); 
                mineWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                mineWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                mineWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                mineWorld.setDifficulty(Difficulty.PEACEFUL);
                plugin.getLogger().info("Monde des mines créé avec succès !");
            }
        } else {
            plugin.getLogger().info("Monde des mines chargé avec succès !");
        }
        this.mineCounter = configManager.getData().getInt("world.mine_counter", 0);
        loadFreeLocations();
    }
    public World getMineWorld() {
        return mineWorld;
    }
    public String getMineWorldName() {
        return this.worldName;
    }
    public Location getNextMineLocation() {
        if (mineWorld == null) {
            plugin.getLogger().warning("Le monde des mines n'est pas chargé");
            return null;
        }
        if (!freeLocations.isEmpty()) {
            Location freeLocation = freeLocations.remove(0);
            if (freeLocation == null || freeLocation.getWorld() == null) {
                plugin.getLogger().warning("Emplacement libre invalide trouvé");
                return null;
            }
            saveFreeLocations();
            plugin.getLogger().info("Réutilisation d'un emplacement de mine libéré: " + freeLocation.toString());
            return freeLocation;
        }
        int gridSize = configManager.getConfig().getInt("Config.Mine-Grid-Size", 100);
        int distance = configManager.getConfig().getInt("Config.Distance-Between-Mines", 10);
        int currentCounter = mineCounter++;
        int gridX = currentCounter % gridSize;
        int gridZ = currentCounter / gridSize;
        int x = gridX * (gridSize + distance);
        int z = gridZ * (gridSize + distance);
        configManager.getData().set("world.mine_counter", mineCounter);
        configManager.saveData();
        return new Location(mineWorld, x, 64, z);
    }
    public void addFreeLocation(Location location) {
        if (location == null) {
            plugin.getLogger().warning("Tentative d'ajout d'un emplacement null");
            return;
        }
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Tentative d'ajout d'un emplacement sans monde");
            return;
        }
        if (!location.getWorld().equals(mineWorld)) {
            plugin.getLogger().warning("Tentative d'ajout d'un emplacement dans un monde incorrect");
            return;
        }
        freeLocations.add(location.clone());
        plugin.getLogger().info("Emplacement de mine ajouté à la liste des emplacements libres: " + location.toString());
        saveFreeLocations();
    }
    private void saveFreeLocations() {
        List<String> locationStrings = new ArrayList<>();
        for (Location loc : freeLocations) {
            locationStrings.add(loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ());
        }
        configManager.getData().set("world.free_locations", locationStrings);
        configManager.saveData();
    }
    private void loadFreeLocations() {
        freeLocations.clear();
        List<String> locationStrings = configManager.getData().getStringList("world.free_locations");
        for (String locStr : locationStrings) {
            String[] parts = locStr.split(",");
            if (parts.length != 4) {
                plugin.getLogger().warning("Format invalide pour un emplacement libre: " + locStr);
                continue;
            }
            try {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Monde non trouvé pour l'emplacement libre: " + worldName);
                    continue;
                }
                Location loc = new Location(world, x, y, z);
                if (loc.getWorld() == null) {
                    plugin.getLogger().warning("Monde invalide pour l'emplacement libre: " + locStr);
                    continue;
                }
                freeLocations.add(loc);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Format invalide pour un emplacement libre: " + locStr);
            }
        }
        plugin.getLogger().info("Chargement de " + freeLocations.size() + " emplacements libres");
    }
    public void unloadWorld() {
        if (mineWorld != null) {
            Bukkit.unloadWorld(mineWorld, true);
        }
    }
} 